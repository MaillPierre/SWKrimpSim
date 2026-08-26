const svg = d3.select("#svg");
const g = svg.append("g");

const fileInput = document.getElementById("file-input");
const filename = document.getElementById("filename");
const status = document.getElementById("status");

const viewVreekenItemButton = document.getElementById("view-itemV-btn"); 
const viewOriginalItemButton = document.getElementById("view-itemO-btn"); 

let viewMode = "itemV"; // view mode: "itemV" or "itemO"

function updateViewModeButtons() {
    viewVreekenItemButton.classList.toggle("active", viewMode === "itemV");
    viewOriginalItemButton.classList.toggle("active", viewMode === "itemO");
}


viewVreekenItemButton.addEventListener("click", () => {setViewMode("itemV");});
viewOriginalItemButton.addEventListener("click", () => {setViewMode("itemO");});

/**
 * Changes the current view mode
 */ 
function setViewMode(mode) { 
    viewMode = mode; 
    if (currentTreeRoot) updateClusterLabels(viewMode, g, currentTreeRoot); 
    updateViewModeButtons(); 
}

const zoomBehavior = d3.zoom()
    .scaleExtent([0.1, 3])
    .on("zoom", event => {g.attr("transform", event.transform);});

svg.call(zoomBehavior);


// JSON loading
fileInput.addEventListener("change", event => {

    const file = event.target.files[0];

    if (!file) return;

    filename.textContent = file.name;
    status.textContent = "Loading...";

    const reader = new FileReader();

    reader.onload = event => {
      try {

          const data = JSON.parse(event.target.result);
          viewMode = "itemV";
          updateViewModeButtons()
          renderTree(data, svg, g, zoomBehavior, viewMode);
      } catch (error) {
          status.textContent = `Error parsing JSON: ${error.message}`;
      }
    };
    reader.readAsText(file);
});

updateViewModeButtons();
