
class FlowInterface extends BaklavaJS.Core.NodeInterface {
  constructor() {
    super("flow", 0);
    this.use(BaklavaJS.InterfaceTypes.setType, "flow");
  }
};

var nodeEditorNodes = [
  BaklavaJS.Core.defineNode({
    type: "TestNode",
    inputs: {
      b: () => new BaklavaJS.RendererVue.TextInputInterface("Hello", "world"),
    },
    outputs: {
      a: () => new BaklavaJS.RendererVue.TextInputInterface("Hello", "world"),
    },
  }),
  BaklavaJS.Core.defineNode({
    type: "TestNode2",
    inputs: {
      in1: () => new BaklavaJS.RendererVue.TextInputInterface("i1", "world"),
      in2: () => new BaklavaJS.RendererVue.IntegerInterface("i2", 0),
    },
    outputs: {
      out1: () => new BaklavaJS.RendererVue.IntegerInterface("o1", 0),
    },
  }),
  BaklavaJS.Core.defineNode({
    type: "Begin",
    inputs: {},
    outputs: {
      next: () => new FlowInterface(),
      references: () => new BaklavaJS.RendererVue.IntegerInterface("references", 0),
    },
  })
];


$(function() {
  Array.from(document.getElementsByClassName("node-editor")).forEach(editorArea => {
    var viewModel = BaklavaJS.createBaklava(editorArea);
    nodeEditorNodes.forEach(node => viewModel.editor.registerNodeType(node));
    var data = JSON.parse(editorArea.nextElementSibling.innerHTML);
    var adjustedHeight = 500;
    data.graph.nodes.forEach(node => {
      height = 150 + node.position.y;
      adjustedHeight = (height > adjustedHeight ? height : adjustedHeight);
    });
    editorArea.style.height = adjustedHeight + "px";
    viewModel.editor.load(data);

    let update = function() {
      editorArea.nextElementSibling.innerHTML = JSON.stringify(viewModel.editor.save());
    };
    viewModel.editor.graphEvents.addNode.subscribe(update, update);
    viewModel.editor.graphEvents.removeNode.subscribe(update, update);
    viewModel.editor.graphEvents.addConnection.subscribe(update, update);
    viewModel.editor.graphEvents.removeConnection.subscribe(update, update);
    viewModel.editor.nodeEvents.update.subscribe(update, update);
    editorArea.addEventListener("mouseout", update);

    let check = function(connection, prevent, graph) {
      prevent();
    }
    viewModel.editor.graphEvents.checkConnection.subscribe(check, check);

    /*
    Array.from(editorArea.getElementsByClassName("node-container")).forEach(container => {
      container.style.transform = "scale(0.7)";
    });
    */
  });
});
