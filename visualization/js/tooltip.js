const tooltip = d3.select("#tooltip");

/**
 * Shows the tooltip for a tree node.
 *
 * @param {MouseEvent} event
 * @param {Object} d - D3 hierarchy node
 */
function showTooltip(event, d) {
    const headItems = viewMode === "itemO"
        ? (d.details ? d.details.map(det => det.originalItem ?? "?") : d.items)
        : d.items;

    const itemsText = headItems.join(", ");
    const fullPatternText = d.details ? d.details.map(det => {
        const originalItem = det.originalItem ?? "?";
        const uri = det.uri ?? "?";
        const type = det.type ?? "?";
        return `${det.item} - ${originalItem} &nbsp;&nbsp; ${uri} &nbsp;&nbsp; ${type}`;  
    }).join("<br>") : "";

    tooltip
        .html(`
            <div class="head">${itemsText}</div>
            <div class="row">
                <span>Item value</span>
                <span>${fullPatternText}</span>
            </div>
            <br>
            <div class="row">
                <span>usage</span>
                <span>${d.usage}</span>
            </div>
            <div class="row">
                <span>items in chain</span>
                <span>${d.items ? d.items.length : 0}</span>
            </div>
            <br>
            <div class="patterns-block">
                <div class="row-title">Patterns</div>
                ${renderPatterns(d)}
            </div>
        `)
    .style("display", "block");

    moveTooltip(event);
}

/**
 * Builds the HTML for the patterns list
 */
function renderPatterns(d) {
    if (!d.patterns || d.patterns.length === 0) {
        return `<div class="pattern-line pattern-empty">-</div>`;
    }

    const useOriginal = viewMode === "itemO";
    const nodeItemsSet = new Set(useOriginal
            ? (d.details ? d.details.map(det => det.originalItem ?? "?") : (d.nodeItems ?? d.items ?? []))
            : (d.nodeItems ?? d.items ?? [])
    );
 
    return d.patterns.map(pattern => {
        const color = colorScale ? colorScale(pattern.usage) : "#ffffff"
        const patternItems = useOriginal ? (pattern.originalItems ?? pattern.items) : pattern.items;
 
        const itemsHtml = patternItems.map(item => {
            if (nodeItemsSet.has(item)) {
                return `<span class="pattern-item pattern-item-node">${item}</span>`;
            }
            return `<span class="pattern-item" style="color:${color}">${item}</span>`;
        }).join(" ");
 
        return `
            <div class="pattern-line">
                <span class="pattern-items">${itemsHtml}</span>
            </div>
        `;
    }).join("");
}


/**
 * Moves the tooltip next to the mouse cursor.
 *
 * @param {MouseEvent} event
 */
function moveTooltip(event) {
  tooltip.style("left", `${event.clientX + 16}px`)
         .style("top", `${event.clientY + 16}px`);
}

/**
 * Hides the tooltip.
 */
function hideTooltip() {
    tooltip.style("display", "none");
}

