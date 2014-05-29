var labelType, useGradients, nativeTextSupport, animate;

(function() {
  var ua = navigator.userAgent,
      iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
      typeOfCanvas = typeof HTMLCanvasElement,
      nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
      textSupport = nativeCanvasSupport 
        && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
  //I'm setting this based on the fact that ExCanvas provides text support for IE
  //and that as of today iPhone/iPad current text support is lame
  labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
  nativeTextSupport = labelType == 'Native';
  useGradients = nativeCanvasSupport;
  animate = !(iStuff || !nativeCanvasSupport);
})();

var Log = {
  elem: false,
  write: function(text){
    if (!this.elem) 
      this.elem = document.getElementById('log');
    this.elem.innerHTML = text;
    this.elem.style.left = (700 - this.elem.offsetWidth / 2) + 'px';
  }
};


var icicle;

function init(){
  //left panel controls
  controls();

  // init data
  var json = {
  
  "id": "<all_packages>",
                   "name": "<All Packages>:222374",
                   "data": {
                        "$area": 222374,
                        "$dim": 222374,
                        "$color": "#3030b0"
                    },"children": [{
"id": "org",
                   "name": "org ∑222374 (985)",
                   "data": {
                        "$area": 222374,
                        "$dim": 222374,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj",
                   "name": "opalj ∑221389 (83)",
                   "data": {
                        "$area": 221389,
                        "$dim": 221389,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/graphs",
                   "name": "graphs ∑433 (433)",
                   "data": {
                        "$area": 433,
                        "$dim": 433,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/av",
                   "name": "av ∑5468 (4572)",
                   "data": {
                        "$area": 5468,
                        "$dim": 5468,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/av/viz",
                   "name": "viz ∑896 (896)",
                   "data": {
                        "$area": 896,
                        "$dim": 896,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/bi",
                   "name": "bi ∑8228 (3695)",
                   "data": {
                        "$area": 8228,
                        "$dim": 8228,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/bi/reader",
                   "name": "reader ∑4533 (4533)",
                   "data": {
                        "$area": 4533,
                        "$dim": 4533,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/br",
                   "name": "br ∑94973 (34262)",
                   "data": {
                        "$area": 94973,
                        "$dim": 94973,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/br/instructions",
                   "name": "instructions ∑26501 (26501)",
                   "data": {
                        "$area": 26501,
                        "$dim": 26501,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/br/ai/taint",
                   "name": "ai.taint ∑47 (47)",
                   "data": {
                        "$area": 47,
                        "$dim": 47,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/br/reader",
                   "name": "reader ∑16656 (16656)",
                   "data": {
                        "$area": 16656,
                        "$dim": 16656,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/br/analyses",
                   "name": "analyses ∑17507 (15766)",
                   "data": {
                        "$area": 17507,
                        "$dim": 17507,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/br/analyses/observers",
                   "name": "observers ∑1687 (1687)",
                   "data": {
                        "$area": 1687,
                        "$dim": 1687,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/br/analyses/ioc",
                   "name": "ioc ∑54 (54)",
                   "data": {
                        "$area": 54,
                        "$dim": 54,
                        "$color": "#80c080"
                    }}]
}]
},{
"id": "org/opalj/frb",
                   "name": "frb ∑25364 (1150)",
                   "data": {
                        "$area": 25364,
                        "$dim": 25364,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/frb/analyses",
                   "name": "analyses ∑23326 (23326)",
                   "data": {
                        "$area": 23326,
                        "$dim": 23326,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/frb/cli",
                   "name": "cli ∑888 (888)",
                   "data": {
                        "$area": 888,
                        "$dim": 888,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/collection",
                   "name": "collection ∑4710 (507)",
                   "data": {
                        "$area": 4710,
                        "$dim": 4710,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/collection/immutable",
                   "name": "immutable ∑2642 (2642)",
                   "data": {
                        "$area": 2642,
                        "$dim": 2642,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/collection/mutable",
                   "name": "mutable ∑1561 (1561)",
                   "data": {
                        "$area": 1561,
                        "$dim": 1561,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/da",
                   "name": "da ∑5039 (5039)",
                   "data": {
                        "$area": 5039,
                        "$dim": 5039,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/util",
                   "name": "util ∑1507 (1507)",
                   "data": {
                        "$area": 1507,
                        "$dim": 1507,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai",
                   "name": "ai ∑70554 (18297)",
                   "data": {
                        "$area": 70554,
                        "$dim": 70554,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/ai/dataflow",
                   "name": "dataflow ∑4477 (3180)",
                   "data": {
                        "$area": 4477,
                        "$dim": 4477,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/ai/dataflow/spec",
                   "name": "spec ∑1291 (1291)",
                   "data": {
                        "$area": 1291,
                        "$dim": 1291,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/dataflow/solver",
                   "name": "solver ∑6 (6)",
                   "data": {
                        "$area": 6,
                        "$dim": 6,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/ai/domain",
                   "name": "domain ∑30490 (1562)",
                   "data": {
                        "$area": 30490,
                        "$dim": 30490,
                        "$color": "#80c080"
                    },"children": [{
"id": "org/opalj/ai/domain/l2",
                   "name": "l2 ∑410 (410)",
                   "data": {
                        "$area": 410,
                        "$dim": 410,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/domain/l0",
                   "name": "l0 ∑11599 (11599)",
                   "data": {
                        "$area": 11599,
                        "$dim": 11599,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/domain/l1",
                   "name": "l1 ∑16432 (16432)",
                   "data": {
                        "$area": 16432,
                        "$dim": 16432,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/domain/tracing",
                   "name": "tracing ∑487 (487)",
                   "data": {
                        "$area": 487,
                        "$dim": 487,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/ai/debug",
                   "name": "debug ∑8153 (8153)",
                   "data": {
                        "$area": 8153,
                        "$dim": 8153,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/project",
                   "name": "project ∑8878 (8878)",
                   "data": {
                        "$area": 8878,
                        "$dim": 8878,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/util",
                   "name": "util ∑105 (105)",
                   "data": {
                        "$area": 105,
                        "$dim": 105,
                        "$color": "#80c080"
                    }},{
"id": "org/opalj/ai/invokedynamic",
                   "name": "invokedynamic ∑154 (154)",
                   "data": {
                        "$area": 154,
                        "$dim": 154,
                        "$color": "#80c080"
                    }}]
},{
"id": "org/opalj/de",
                   "name": "de ∑5030 (5030)",
                   "data": {
                        "$area": 5030,
                        "$dim": 5030,
                        "$color": "#80c080"
                    }}]
}]
}]
		
  };
  // end
  // init Icicle
  icicle = new $jit.Icicle({
    // id of the visualization container
    injectInto: 'infovis',
    // whether to add transition animations
    animate: animate,
    // nodes offset
    offset: 1,
    // whether to add cushion type nodes
    cushion: false,
    //show only three levels at a time
    constrained: true,
    levelsToShow: 3,
    // enable tips
    Tips: {
      enable: true,
      type: 'Native',
      // add positioning offsets
      offsetX: 20,
      offsetY: 20,
      // implement the onShow method to
      // add content to the tooltip when a node
      // is hovered
      onShow: function(tip, node){
        // count children
        var count = 0;
        node.eachSubnode(function(){
          count++;
        });
        // add tooltip info
        tip.innerHTML = "<div class=\"tip-title\"><b>Name:</b> " + node.name
            + "</div><div class=\"tip-text\">" + count + " children</div>";
      }
    },
    // Add events to nodes
    Events: {
      enable: true,
      onMouseEnter: function(node) {
        //add border and replot node
        node.setData('border', '#33dddd');
        icicle.fx.plotNode(node, icicle.canvas);
        icicle.labels.plotLabel(icicle.canvas, node, icicle.controller);
      },
      onMouseLeave: function(node) {
        node.removeData('border');
        icicle.fx.plot();
      },
      onClick: function(node){
        if (node) {
          //hide tips and selections
          icicle.tips.hide();
          if(icicle.events.hovered)
            this.onMouseLeave(icicle.events.hovered);
          //perform the enter animation
          icicle.enter(node);
        }
      },
      onRightClick: function(){
        //hide tips and selections
        icicle.tips.hide();
        if(icicle.events.hovered)
          this.onMouseLeave(icicle.events.hovered);
        //perform the out animation
        icicle.out();
      }
    },
    // Add canvas label styling
    Label: {
      type: labelType // "Native" or "HTML"
    },
    // Add the name of the node in the corresponding label
    // This method is called once, on label creation and only for DOM and not
    // Native labels.
    onCreateLabel: function(domElement, node){
      domElement.innerHTML = node.name;
      var style = domElement.style;
      style.fontSize = '0.9em';
      style.display = '';
      style.cursor = 'pointer';
      style.color = '#333';
      style.overflow = 'hidden';
    },
    // Change some label dom properties.
    // This method is called each time a label is plotted.
    onPlaceLabel: function(domElement, node){
      var style = domElement.style,
          width = node.getData('width'),
          height = node.getData('height');
      if(width < 7 || height < 7) {
        style.display = 'none';
      } else {
        style.display = '';
        style.width = width + 'px';
        style.height = height + 'px';
      }
    }
  });
  // load data
  icicle.loadJSON(json);
  // compute positions and plot
  icicle.refresh();
  //end
}

//init controls
function controls() {
  var jit = $jit;
  var gotoparent = jit.id('update');
  jit.util.addEvent(gotoparent, 'click', function() {
    icicle.out();
  });
  var select = jit.id('s-orientation');
  jit.util.addEvent(select, 'change', function () {
    icicle.layout.orientation = select[select.selectedIndex].value;
    icicle.refresh();
  });
  var levelsToShowSelect = jit.id('i-levels-to-show');
  jit.util.addEvent(levelsToShowSelect, 'change', function () {
    var index = levelsToShowSelect.selectedIndex;
    if(index == 0) {
      icicle.config.constrained = false;
    } else {
      icicle.config.constrained = true;
      icicle.config.levelsToShow = index;
    }
    icicle.refresh();
  });
}
//end
