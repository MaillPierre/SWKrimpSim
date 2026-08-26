const NODE_SEP_Y_MIN = 70; // min vertical gap between nodes in the same column
const NODE_SEP_GAP = 30; // extra gap added to the tallest node's height
const LEVEL_SEP_X = 60; // horizontal gap between columns
const COMPONENT_GAP = 50; // vertical gap between disconnected graphs

const CHAR_WIDTH = 8.2; // approx width in px of one text character
const CLUSTER_PAD_X = 22; // horizontal padding inside a node box
const CLUSTER_MIN_HEIGHT = 44; // minimum box height
const CLUSTER_MIN_WIDTH = 60; // minimum box width

const LABEL_LINE_HEIGHT = 14; // height of one text line
const LABEL_USAGE_GAP = 18; // gap between last text line and the usage line
const CLUSTER_VPAD = 8; // vertical padding inside a node box

const RING_GAP = 8; // offset of the start ring node

const START_ARROW_LEN = 26; // length of the incoming arrow for start nodes
const START_ARROW_GAP = 3; // small gap between the arrowhead and the box edge

let currentTreeRoot = null;
let colorScale = null;

/**
 * Returns the label that should be displayed inside a cluster,
 * one entry per line, depending on the selected view mode
 */
function getClusterLabelLines(data, viewMode) {
    if (!data.details) return [data.items ? data.items.join(",") : ""];
    switch (viewMode) {
        case "itemO":
            return  [data.details.map(d => d.originalItem ?? "?").join(",")]
        case "itemV":
        default:
            return [data.items ? data.items.join(",") : ""];
    }
}

/**
 * Computes the vertical layout of a cluster's text content
 * Text center around 0
 */
function verticalLayout(lineCount) {

    const lineYsRaw = Array.from(
        { length: lineCount },
        (_, i) => (i - (lineCount - 1) / 2) * LABEL_LINE_HEIGHT
    );

    const lastLineYRaw = lineYsRaw[lineCount - 1];
    const usageYRaw = lastLineYRaw + LABEL_USAGE_GAP;

    const topRaw = lineYsRaw[0] - LABEL_LINE_HEIGHT / 2 - CLUSTER_VPAD;
    const bottomRaw = usageYRaw + LABEL_LINE_HEIGHT / 2 + CLUSTER_VPAD;

    const rawHeight = bottomRaw - topRaw;
    const height = Math.max(CLUSTER_MIN_HEIGHT, rawHeight);

    const midpoint = (topRaw + bottomRaw) / 2;

    return {
        lineYs: lineYsRaw.map(y => y - midpoint),
        usageY: usageYRaw - midpoint,
        height
    };
}

/**
 * Computes the width of a cluster
 */
function clusterWidth(d, viewMode = "itemV") {
    const lines = getClusterLabelLines(d, viewMode);
    const usageText = `usage:${d.usage}`;

    const longestLine = Math.max(0, ...lines.map(line => line.length));
    const longest = Math.max(longestLine, usageText.length);

    return Math.max(CLUSTER_MIN_WIDTH, longest * CHAR_WIDTH + CLUSTER_PAD_X * 2);
}

/**
 * Computes the height of a cluster
 */
function clusterHeight(d, viewMode = "itemV") {
    const lines = getClusterLabelLines(d, viewMode);
    return verticalLayout(lines.length).height;
}

/**
 * Renders the item-label <text> as one or more vertically stacked
 * <tspan> lines and repositions the usage-label right below them
 */
function renderClusterLabel(nodeGroupSelection, viewMode) {

    nodeGroupSelection.each(function (d) {

        const lines = getClusterLabelLines(d, viewMode);
        const layout = verticalLayout(lines.length);
        const sel = d3.select(this); // <g.node>

        const itemLabel = sel.select("text.item-label");

        lines.forEach((line, i) => {
            itemLabel.attr("x", 0)
                    .attr("y", layout.lineYs[i])
                    .text(line);
        });

        sel.select("text.usage-label")
            .attr("y", layout.usageY)
            .text(`usage:${d.usage}`);
    });
}

/**
 * Assigns each node a depth equal to the length of the longest path
 * reaching it from any root node
 */
function assignDepths(nodes, links) {
    nodes.forEach(n => n.depth = 0);
    const maxIterations = nodes.length + 1;
    let iterations = 0;
    let changed = true;
    while (changed && iterations < maxIterations) {
        changed = false;
        links.forEach(link => {
            if (link.sourceObj && link.targetObj) {
                if (link.targetObj.depth < link.sourceObj.depth + 1) {
                    link.targetObj.depth = link.sourceObj.depth + 1;
                    changed = true;
                }
            }
        });
        iterations++;
    }
}

/**
 * Groups nodes into connected components
 */
function computeComponents(nodes, links) {

    const adjacency = new Map();
    nodes.forEach(n => adjacency.set(n.id, []));

    // create relation source->target and viceversa
    links.forEach(l => {
        if (!l.sourceObj || !l.targetObj) return;
        adjacency.get(l.sourceObj.id).push(l.targetObj.id);
        adjacency.get(l.targetObj.id).push(l.sourceObj.id);
    });

    const nodeById = new Map(nodes.map(n => [n.id, n]));
    const visited = new Set();
    const components = [];

    nodes.forEach(start => {
        if (visited.has(start.id)) return;

        const compNodes = [];
        const stack = [start.id];
        visited.add(start.id);

        while (stack.length) {
            const id = stack.pop();
            compNodes.push(nodeById.get(id));

            adjacency.get(id).forEach(neighborId => {
                if (!visited.has(neighborId)) {
                    visited.add(neighborId);
                    stack.push(neighborId);
                }
            });
        }
        components.push(compNodes);
    });

    return components;
}

/**
 * Lays out a single connected component
 * - columns by depth
 * - rows within each column
 * Centered around x = 0
 */
function layoutComponent(componentNodes, componentLinks, nodeSepY) {

    const levels = d3.groups(componentNodes, d => d.depth); // [depth, [nodes with that depth]]
    levels.sort((a, b) => a[0] - b[0]); // ascending depth
    let currentY = 0;

    levels.forEach(([depth, levelNodes]) => {
        const maxWidth = d3.max(levelNodes, d => d.clusterWidth) || CLUSTER_MIN_WIDTH;

        if (depth === 0) {
            currentY = maxWidth / 2;
        } else {
            const prevNodes = componentNodes.filter(n => n.depth === depth - 1);
            const prevMaxWidth = d3.max(prevNodes, n => n.clusterWidth) || CLUSTER_MIN_WIDTH;
            currentY += prevMaxWidth / 2 + LEVEL_SEP_X + maxWidth / 2;
        }

        // for long links
        const bypassLinks = componentLinks.filter(l => l.targetObj.depth - l.sourceObj.depth > 1);
        const isBypassed = bypassLinks.some(l => l.sourceObj.depth < depth && l.targetObj.depth > depth);

        // total height occupied by all nodes
        const totalHeight = (levelNodes.length - 1) * nodeSepY;

        levelNodes.forEach((node, i) => {
            node.y = currentY;
            let baseX = i * nodeSepY - totalHeight / 2;

            // make space for the long links
            if (isBypassed) {
                baseX -= 80;
            }
            node.x = baseX;
        });
    });
}

/**
 * Positions every node (grouped by connected graph) and draws all the links
 */
function updateGraphLayout(viewMode, root, g) {
    const nodes = root.nodes || [];
    const links = root.links || [];

    nodes.forEach(d => {
        d.clusterWidth = clusterWidth(d, viewMode);
        d.clusterHeight = clusterHeight(d, viewMode);
    });

    const maxClusterHeight = d3.max(nodes, d => d.clusterHeight) || CLUSTER_MIN_HEIGHT;
    const nodeSepY = Math.max(NODE_SEP_Y_MIN, maxClusterHeight + NODE_SEP_GAP);

    const components = computeComponents(nodes, links);

    let bottomEdge = null; // from last component

    components.forEach(componentNodes => {

        const componentIds = new Set(componentNodes.map(n => n.id));
        const componentLinks = links.filter(l => l.sourceObj && l.targetObj && componentIds.has(l.sourceObj.id));

        layoutComponent(componentNodes, componentLinks, nodeSepY);

        // component position
        const top = d3.min(componentNodes, n => n.x - n.clusterHeight / 2);
        const bottom = d3.max(componentNodes, n => n.x + n.clusterHeight / 2);

        if (bottomEdge === null) {
            // center in 0
            bottomEdge = bottom;
        } else {
            const offset = (bottomEdge + COMPONENT_GAP) - top;
            componentNodes.forEach(n => { n.x += offset; });
            bottomEdge = bottom + offset;
        }
    });

    g.selectAll("g.node").attr("transform", d => `translate(${d.y},${d.x})`);

    g.selectAll("rect.cluster")
        .attr("x", d => -clusterWidth(d, viewMode) / 2)
        .attr("y", d => -clusterHeight(d, viewMode) / 2)
        .attr("width", d => clusterWidth(d, viewMode))
        .attr("height", d => clusterHeight(d, viewMode));

    // Incoming arrow for start nodes
    g.selectAll("line.start-arrow")
        .attr("x1", d => -clusterWidth(d, viewMode) / 2 - START_ARROW_LEN)
        .attr("y1", 0)
        .attr("x2", d => -clusterWidth(d, viewMode) / 2 - START_ARROW_GAP)
        .attr("y2", 0);
    
    // Extra ring for end nodes
    g.selectAll("rect.cluster-ring-end")
        .attr("x", d => -clusterWidth(d, viewMode) / 2 - RING_GAP)
        .attr("y", d => -clusterHeight(d, viewMode) / 2 - RING_GAP)
        .attr("width", d => clusterWidth(d, viewMode) + RING_GAP * 2)
        .attr("height", d => clusterHeight(d, viewMode) + RING_GAP * 2)
        .attr("rx", d => clusterHeight(d, viewMode) / 2 + RING_GAP)
        .attr("ry", d => clusterHeight(d, viewMode) / 2 + RING_GAP);

    // like Bezier curve
    const linkGenerator = d3.linkHorizontal().x(d => d.y).y(d => d.x);

    // link position
    g.selectAll("path.link")
        .attr("d", d => {
            const srcWidth = clusterWidth(d.sourceObj, viewMode) / 2;
            const tgtWidth = clusterWidth(d.targetObj, viewMode) / 2;

            const srcPt = { x: d.sourceObj.y + srcWidth, y: d.sourceObj.x };
            const tgtPt = { x: d.targetObj.y - tgtWidth, y: d.targetObj.x };

            if (d.targetObj.depth - d.sourceObj.depth > 1) {
                const midX = (srcPt.x + tgtPt.x) / 2;
                const midY = Math.max(srcPt.y, tgtPt.y) + 85;
                return `M ${srcPt.x} ${srcPt.y} Q ${midX} ${midY} ${tgtPt.x} ${tgtPt.y}`;
            }

            return linkGenerator({
                source: { x: srcPt.y, y: srcPt.x },
                target: { x: tgtPt.y, y: tgtPt.x }
            });
        });
}

/**
 * Updates the labels and recalculates the complete layout
 */
function updateClusterLabels(viewMode, g, root) {
    renderClusterLabel(g.selectAll("g.node"), viewMode);
    updateGraphLayout(viewMode, root, g);
}

/**
 * Creates the marker arrowhead definition used by start-node arrows
 */
function ensureStartArrowMarker(svg) {
    let defs = svg.select("defs");
    if (defs.empty()) {
        defs = svg.append("defs");
    }
    defs.select("marker#start-arrowhead").remove();

    defs.append("marker")
        .attr("id", "start-arrowhead")
        .attr("viewBox", "0 0 10 10")
        .attr("refX", 9)
        .attr("refY", 5)
        .attr("markerWidth", 7)
        .attr("markerHeight", 7)
        .attr("orient", "auto-start-reverse")
        .append("path")
        .attr("d", "M0,0 L10,5 L0,10 z")
        .style("fill", "context-stroke");
}

/**
 * Buils the whole graph from scratch. Creates the DOM elements and draws them
 */
function renderTree(data, svg, g, zoomBehavior, viewMode) {
    document.getElementById("empty-state").style.display = "none";
    svg.style("display", "block");
    g.selectAll("*").remove();

    currentTreeRoot = data;

    const nodes = data.nodes || [];
    const links = data.links || [];

    const nodeMap = new Map(nodes.map(n => [n.id, n]));
    links.forEach(l => {
        l.sourceObj = nodeMap.get(l.source);
        l.targetObj = nodeMap.get(l.target);
    });

    assignDepths(nodes, links);

    const maxUsage = d3.max(nodes, d => d.usage) || 1;

    colorScale = d3.scaleSequential()
        .domain([0, maxUsage])
        .interpolator(d3.interpolateRgbBasis([getVar("--accent-hot"), getVar("--accent-cold")]));

    ensureStartArrowMarker(svg);
    
    g.append("g")
        .attr("class", "links")
        .selectAll("path")
        .data(links)
        .join("path")
        .attr("class", "link");

    const nodeGroup = g.append("g")
        .attr("class", "nodes")
        .selectAll("g")
        .data(nodes)
        .join("g")
        .attr("class", "node")
        .on("mouseenter", showTooltip)
        .on("mousemove", moveTooltip)
        .on("mouseleave", hideTooltip);

    nodeGroup.append("rect")
        .attr("class", "cluster")
        .attr("rx", CLUSTER_MIN_HEIGHT / 2)
        .attr("ry", CLUSTER_MIN_HEIGHT / 2)
        .attr("fill", d => colorScale(d.usage))
        .attr("fill-opacity", 0.16)
        .attr("stroke", d => colorScale(d.usage));

    nodeGroup.filter(d => d.isStart)
        .append("line")
        .attr("class", "start-arrow")
        .attr("stroke", d => colorScale(d.usage))
        .attr("stroke-width", 2)
        .attr("marker-end", "url(#start-arrowhead)")

    nodeGroup.filter(d => d.isEnd)
        .append("rect")
        .attr("class", "cluster-ring cluster-ring-end")
        .attr("fill", "none")
        .attr("stroke", d => colorScale(d.usage))
        .attr("stroke-dasharray", "4 3");

    nodeGroup.append("text").attr("class", "item-label");
    nodeGroup.append("text").attr("class", "usage-label");

    renderClusterLabel(nodeGroup, viewMode);

    // center the initial view on the first group
    const initialTransform = d3.zoomIdentity.translate(60, window.innerHeight / 2 - 56).scale(1);
    svg.call(zoomBehavior.transform, initialTransform);

    document.getElementById("status").textContent = `${nodes.length} nodes · max usage: ${maxUsage}`;

    updateGraphLayout(viewMode, data, g);
}

/**
 * CSS -> JS
 */
function getVar(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}
