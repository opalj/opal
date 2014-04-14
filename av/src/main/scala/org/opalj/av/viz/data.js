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
		                   "name": "<All Packages>:5463972",
		                   "data": {
		                        "$area": 5463972,
		                        "$dim": 5463972,
		                        "$color": "#3030b0"
		                    },"children": [{
		"id": "javax",
		                   "name": "javax ∑756519 (0)",
		                   "data": {
		                        "$area": 756519,
		                        "$dim": 756519,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "javax/transaction",
		                   "name": "transaction ∑34 (21)",
		                   "data": {
		                        "$area": 34,
		                        "$dim": 34,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/transaction/xa",
		                   "name": "xa ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/sound/sampled",
		                   "name": "sound.sampled ∑4205 (4041)",
		                   "data": {
		                        "$area": 4205,
		                        "$dim": 4205,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sound/sampled/spi",
		                   "name": "spi ∑164 (164)",
		                   "data": {
		                        "$area": 164,
		                        "$dim": 164,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/net",
		                   "name": "net ∑2349 (126)",
		                   "data": {
		                        "$area": 2349,
		                        "$dim": 2349,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/net/ssl",
		                   "name": "ssl ∑2223 (2223)",
		                   "data": {
		                        "$area": 2223,
		                        "$dim": 2223,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/print",
		                   "name": "print ∑11404 (2798)",
		                   "data": {
		                        "$area": 11404,
		                        "$dim": 11404,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/print/event",
		                   "name": "event ∑68 (68)",
		                   "data": {
		                        "$area": 68,
		                        "$dim": 68,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/print/attribute",
		                   "name": "attribute ∑8538 (2326)",
		                   "data": {
		                        "$area": 8538,
		                        "$dim": 8538,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/print/attribute/standard",
		                   "name": "standard ∑6212 (6212)",
		                   "data": {
		                        "$area": 6212,
		                        "$dim": 6212,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "javax/security/cert",
		                   "name": "security.cert ∑260 (260)",
		                   "data": {
		                        "$area": 260,
		                        "$dim": 260,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/sound/midi",
		                   "name": "sound.midi ∑2850 (2776)",
		                   "data": {
		                        "$area": 2850,
		                        "$dim": 2850,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sound/midi/spi",
		                   "name": "spi ∑74 (74)",
		                   "data": {
		                        "$area": 74,
		                        "$dim": 74,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/management",
		                   "name": "management ∑76954 (14395)",
		                   "data": {
		                        "$area": 76954,
		                        "$dim": 76954,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/management/monitor",
		                   "name": "monitor ∑4368 (4368)",
		                   "data": {
		                        "$area": 4368,
		                        "$dim": 4368,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/timer",
		                   "name": "timer ∑1393 (1393)",
		                   "data": {
		                        "$area": 1393,
		                        "$dim": 1393,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/relation",
		                   "name": "relation ∑9815 (9815)",
		                   "data": {
		                        "$area": 9815,
		                        "$dim": 9815,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/openmbean",
		                   "name": "openmbean ∑6572 (6572)",
		                   "data": {
		                        "$area": 6572,
		                        "$dim": 6572,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/modelmbean",
		                   "name": "modelmbean ∑11295 (11295)",
		                   "data": {
		                        "$area": 11295,
		                        "$dim": 11295,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/loading",
		                   "name": "loading ∑2795 (2795)",
		                   "data": {
		                        "$area": 2795,
		                        "$dim": 2795,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/remote",
		                   "name": "remote ∑26321 (2277)",
		                   "data": {
		                        "$area": 26321,
		                        "$dim": 26321,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/management/remote/rmi",
		                   "name": "rmi ∑24044 (24044)",
		                   "data": {
		                        "$area": 24044,
		                        "$dim": 24044,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "javax/activity",
		                   "name": "activity ∑51 (51)",
		                   "data": {
		                        "$area": 51,
		                        "$dim": 51,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/accessibility",
		                   "name": "accessibility ∑2398 (2398)",
		                   "data": {
		                        "$area": 2398,
		                        "$dim": 2398,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/activation",
		                   "name": "activation ∑3993 (3993)",
		                   "data": {
		                        "$area": 3993,
		                        "$dim": 3993,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming",
		                   "name": "naming ∑10169 (3800)",
		                   "data": {
		                        "$area": 10169,
		                        "$dim": 10169,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/naming/event",
		                   "name": "event ∑73 (73)",
		                   "data": {
		                        "$area": 73,
		                        "$dim": 73,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/spi",
		                   "name": "spi ∑1724 (1724)",
		                   "data": {
		                        "$area": 1724,
		                        "$dim": 1724,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/directory",
		                   "name": "directory ∑1493 (1493)",
		                   "data": {
		                        "$area": 1493,
		                        "$dim": 1493,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/ldap",
		                   "name": "ldap ∑3079 (3079)",
		                   "data": {
		                        "$area": 3079,
		                        "$dim": 3079,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/lang/model",
		                   "name": "lang.model ∑2573 (465)",
		                   "data": {
		                        "$area": 2573,
		                        "$dim": 2573,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/lang/model/element",
		                   "name": "element ∑472 (472)",
		                   "data": {
		                        "$area": 472,
		                        "$dim": 472,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/lang/model/util",
		                   "name": "util ∑1241 (1241)",
		                   "data": {
		                        "$area": 1241,
		                        "$dim": 1241,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/lang/model/type",
		                   "name": "type ∑395 (395)",
		                   "data": {
		                        "$area": 395,
		                        "$dim": 395,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/security/sasl",
		                   "name": "security.sasl ∑502 (502)",
		                   "data": {
		                        "$area": 502,
		                        "$dim": 502,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/sql",
		                   "name": "sql ∑7084 (50)",
		                   "data": {
		                        "$area": 7084,
		                        "$dim": 7084,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sql/rowset",
		                   "name": "rowset ∑7034 (2577)",
		                   "data": {
		                        "$area": 7034,
		                        "$dim": 7034,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sql/rowset/serial",
		                   "name": "serial ∑3490 (3490)",
		                   "data": {
		                        "$area": 3490,
		                        "$dim": 3490,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/sql/rowset/spi",
		                   "name": "spi ∑967 (967)",
		                   "data": {
		                        "$area": 967,
		                        "$dim": 967,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "javax/rmi",
		                   "name": "rmi ∑1357 (281)",
		                   "data": {
		                        "$area": 1357,
		                        "$dim": 1357,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/rmi/CORBA",
		                   "name": "CORBA ∑588 (588)",
		                   "data": {
		                        "$area": 588,
		                        "$dim": 588,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/rmi/ssl",
		                   "name": "ssl ∑488 (488)",
		                   "data": {
		                        "$area": 488,
		                        "$dim": 488,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/imageio",
		                   "name": "imageio ∑20867 (6925)",
		                   "data": {
		                        "$area": 20867,
		                        "$dim": 20867,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/imageio/plugins/bmp",
		                   "name": "plugins.bmp ∑33 (33)",
		                   "data": {
		                        "$area": 33,
		                        "$dim": 33,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/plugins/jpeg",
		                   "name": "plugins.jpeg ∑3502 (3502)",
		                   "data": {
		                        "$area": 3502,
		                        "$dim": 3502,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/metadata",
		                   "name": "metadata ∑2782 (2782)",
		                   "data": {
		                        "$area": 2782,
		                        "$dim": 2782,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/stream",
		                   "name": "stream ∑5608 (5608)",
		                   "data": {
		                        "$area": 5608,
		                        "$dim": 5608,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/spi",
		                   "name": "spi ∑2017 (2017)",
		                   "data": {
		                        "$area": 2017,
		                        "$dim": 2017,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/tools",
		                   "name": "tools ∑1033 (1033)",
		                   "data": {
		                        "$area": 1033,
		                        "$dim": 1033,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml",
		                   "name": "xml ∑19796 (3)",
		                   "data": {
		                        "$area": 19796,
		                        "$dim": 19796,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/parsers",
		                   "name": "parsers ∑1337 (1337)",
		                   "data": {
		                        "$area": 1337,
		                        "$dim": 1337,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/namespace",
		                   "name": "namespace ∑240 (240)",
		                   "data": {
		                        "$area": 240,
		                        "$dim": 240,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws",
		                   "name": "ws ∑1325 (347)",
		                   "data": {
		                        "$area": 1325,
		                        "$dim": 1325,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/ws/spi",
		                   "name": "spi ∑507 (494)",
		                   "data": {
		                        "$area": 507,
		                        "$dim": 507,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/ws/spi/http",
		                   "name": "http ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/ws/wsaddressing",
		                   "name": "wsaddressing ∑258 (258)",
		                   "data": {
		                        "$area": 258,
		                        "$dim": 258,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/http",
		                   "name": "http ∑9 (9)",
		                   "data": {
		                        "$area": 9,
		                        "$dim": 9,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/soap",
		                   "name": "soap ∑166 (166)",
		                   "data": {
		                        "$area": 166,
		                        "$dim": 166,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/handler",
		                   "name": "handler ∑38 (38)",
		                   "data": {
		                        "$area": 38,
		                        "$dim": 38,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/datatype",
		                   "name": "datatype ∑1615 (1615)",
		                   "data": {
		                        "$area": 1615,
		                        "$dim": 1615,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/xpath",
		                   "name": "xpath ∑1242 (1242)",
		                   "data": {
		                        "$area": 1242,
		                        "$dim": 1242,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/validation",
		                   "name": "validation ∑1260 (1260)",
		                   "data": {
		                        "$area": 1260,
		                        "$dim": 1260,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/stream",
		                   "name": "stream ∑1145 (876)",
		                   "data": {
		                        "$area": 1145,
		                        "$dim": 1145,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/stream/util",
		                   "name": "util ∑269 (269)",
		                   "data": {
		                        "$area": 269,
		                        "$dim": 269,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/soap",
		                   "name": "soap ∑1038 (1038)",
		                   "data": {
		                        "$area": 1038,
		                        "$dim": 1038,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto",
		                   "name": "crypto ∑1744 (287)",
		                   "data": {
		                        "$area": 1744,
		                        "$dim": 1744,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/crypto/dsig",
		                   "name": "dsig ∑1244 (601)",
		                   "data": {
		                        "$area": 1244,
		                        "$dim": 1244,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/crypto/dsig/keyinfo",
		                   "name": "keyinfo ∑131 (131)",
		                   "data": {
		                        "$area": 131,
		                        "$dim": 131,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto/dsig/dom",
		                   "name": "dom ∑198 (198)",
		                   "data": {
		                        "$area": 198,
		                        "$dim": 198,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto/dsig/spec",
		                   "name": "spec ∑314 (314)",
		                   "data": {
		                        "$area": 314,
		                        "$dim": 314,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/crypto/dom",
		                   "name": "dom ∑213 (213)",
		                   "data": {
		                        "$area": 213,
		                        "$dim": 213,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/transform",
		                   "name": "transform ∑1768 (1108)",
		                   "data": {
		                        "$area": 1768,
		                        "$dim": 1768,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/transform/stax",
		                   "name": "stax ∑156 (156)",
		                   "data": {
		                        "$area": 156,
		                        "$dim": 156,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/stream",
		                   "name": "stream ∑137 (137)",
		                   "data": {
		                        "$area": 137,
		                        "$dim": 137,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/sax",
		                   "name": "sax ∑125 (125)",
		                   "data": {
		                        "$area": 125,
		                        "$dim": 125,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/dom",
		                   "name": "dom ∑242 (242)",
		                   "data": {
		                        "$area": 242,
		                        "$dim": 242,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/bind",
		                   "name": "bind ∑7079 (4516)",
		                   "data": {
		                        "$area": 7079,
		                        "$dim": 7079,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/bind/util",
		                   "name": "util ∑370 (370)",
		                   "data": {
		                        "$area": 370,
		                        "$dim": 370,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/helpers",
		                   "name": "helpers ∑1679 (1679)",
		                   "data": {
		                        "$area": 1679,
		                        "$dim": 1679,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/attachment",
		                   "name": "attachment ∑10 (10)",
		                   "data": {
		                        "$area": 10,
		                        "$dim": 10,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/annotation",
		                   "name": "annotation ∑504 (249)",
		                   "data": {
		                        "$area": 504,
		                        "$dim": 504,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/bind/annotation/adapters",
		                   "name": "adapters ∑255 (255)",
		                   "data": {
		                        "$area": 255,
		                        "$dim": 255,
		                        "$color": "#80c080"
		                    }}]
		}]
		}]
		},{
		"id": "javax/smartcardio",
		                   "name": "smartcardio ∑1819 (1819)",
		                   "data": {
		                        "$area": 1819,
		                        "$dim": 1819,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/jws",
		                   "name": "jws ∑162 (48)",
		                   "data": {
		                        "$area": 162,
		                        "$dim": 162,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/jws/soap",
		                   "name": "soap ∑114 (114)",
		                   "data": {
		                        "$area": 114,
		                        "$dim": 114,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/security/auth",
		                   "name": "security.auth ∑9001 (3438)",
		                   "data": {
		                        "$area": 9001,
		                        "$dim": 9001,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/security/auth/callback",
		                   "name": "callback ∑588 (588)",
		                   "data": {
		                        "$area": 588,
		                        "$dim": 588,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/kerberos",
		                   "name": "kerberos ∑2995 (2995)",
		                   "data": {
		                        "$area": 2995,
		                        "$dim": 2995,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/login",
		                   "name": "login ∑1643 (1643)",
		                   "data": {
		                        "$area": 1643,
		                        "$dim": 1643,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/x500",
		                   "name": "x500 ∑337 (337)",
		                   "data": {
		                        "$area": 337,
		                        "$dim": 337,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/script",
		                   "name": "script ∑1191 (1191)",
		                   "data": {
		                        "$area": 1191,
		                        "$dim": 1191,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/annotation",
		                   "name": "annotation ∑263 (38)",
		                   "data": {
		                        "$area": 263,
		                        "$dim": 263,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/annotation/processing",
		                   "name": "processing ∑225 (225)",
		                   "data": {
		                        "$area": 225,
		                        "$dim": 225,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing",
		                   "name": "swing ∑576204 (116338)",
		                   "data": {
		                        "$area": 576204,
		                        "$dim": 576204,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/border",
		                   "name": "border ∑2945 (2945)",
		                   "data": {
		                        "$area": 2945,
		                        "$dim": 2945,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/undo",
		                   "name": "undo ∑1102 (1102)",
		                   "data": {
		                        "$area": 1102,
		                        "$dim": 1102,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/tree",
		                   "name": "tree ∑10912 (10912)",
		                   "data": {
		                        "$area": 10912,
		                        "$dim": 10912,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/filechooser",
		                   "name": "filechooser ∑1175 (1175)",
		                   "data": {
		                        "$area": 1175,
		                        "$dim": 1175,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/colorchooser",
		                   "name": "colorchooser ∑8989 (8989)",
		                   "data": {
		                        "$area": 8989,
		                        "$dim": 8989,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/table",
		                   "name": "table ∑3740 (3740)",
		                   "data": {
		                        "$area": 3740,
		                        "$dim": 3740,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/text",
		                   "name": "text ∑110764 (52168)",
		                   "data": {
		                        "$area": 110764,
		                        "$dim": 110764,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/text/html",
		                   "name": "html ∑52370 (45152)",
		                   "data": {
		                        "$area": 52370,
		                        "$dim": 52370,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/text/html/parser",
		                   "name": "parser ∑7218 (7218)",
		                   "data": {
		                        "$area": 7218,
		                        "$dim": 7218,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/text/rtf",
		                   "name": "rtf ∑6226 (6226)",
		                   "data": {
		                        "$area": 6226,
		                        "$dim": 6226,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/plaf",
		                   "name": "plaf ∑318870 (724)",
		                   "data": {
		                        "$area": 318870,
		                        "$dim": 318870,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/plaf/basic",
		                   "name": "basic ∑99826 (99826)",
		                   "data": {
		                        "$area": 99826,
		                        "$dim": 99826,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/multi",
		                   "name": "multi ∑10326 (10326)",
		                   "data": {
		                        "$area": 10326,
		                        "$dim": 10326,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/synth",
		                   "name": "synth ∑41219 (41219)",
		                   "data": {
		                        "$area": 41219,
		                        "$dim": 41219,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/nimbus",
		                   "name": "nimbus ∑124742 (124742)",
		                   "data": {
		                        "$area": 124742,
		                        "$dim": 124742,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/metal",
		                   "name": "metal ∑42033 (42033)",
		                   "data": {
		                        "$area": 42033,
		                        "$dim": 42033,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/event",
		                   "name": "event ∑1369 (1369)",
		                   "data": {
		                        "$area": 1369,
		                        "$dim": 1369,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com",
		                   "name": "com ∑1875758 (0)",
		                   "data": {
		                        "$area": 1875758,
		                        "$dim": 1875758,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "com/sun/java/util/jar/pack",
		                   "name": "sun.java.util.jar.pack ∑58850 (58850)",
		                   "data": {
		                        "$area": 58850,
		                        "$dim": 58850,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/encoding",
		                   "name": "sun.corba.se.impl.encoding ∑17173 (17173)",
		                   "data": {
		                        "$area": 17173,
		                        "$dim": 17173,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/image/codec/jpeg",
		                   "name": "sun.image.codec.jpeg ∑2640 (2640)",
		                   "data": {
		                        "$area": 2640,
		                        "$dim": 2640,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/internal/CosNaming",
		                   "name": "sun.corba.se.internal.CosNaming ∑140 (140)",
		                   "data": {
		                        "$area": 140,
		                        "$dim": 140,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/logging",
		                   "name": "sun.corba.se.spi.logging ∑103 (103)",
		                   "data": {
		                        "$area": 103,
		                        "$dim": 103,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/url/rmi",
		                   "name": "sun.jndi.url.rmi ∑277 (277)",
		                   "data": {
		                        "$area": 277,
		                        "$dim": 277,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/media/sound",
		                   "name": "sun.media.sound ∑92467 (92467)",
		                   "data": {
		                        "$area": 92467,
		                        "$dim": 92467,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/wsdl",
		                   "name": "sun.xml.internal.ws.wsdl ∑9771 (876)",
		                   "data": {
		                        "$area": 9771,
		                        "$dim": 9771,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/wsdl/parser",
		                   "name": "parser ∑4712 (4712)",
		                   "data": {
		                        "$area": 4712,
		                        "$dim": 4712,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/wsdl/writer",
		                   "name": "writer ∑4183 (4183)",
		                   "data": {
		                        "$area": 4183,
		                        "$dim": 4183,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/imageio/spi",
		                   "name": "sun.imageio.spi ∑190 (190)",
		                   "data": {
		                        "$area": 190,
		                        "$dim": 190,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/ldap",
		                   "name": "sun.jndi.ldap ∑28986 (26596)",
		                   "data": {
		                        "$area": 28986,
		                        "$dim": 28986,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/jndi/ldap/sasl",
		                   "name": "sasl ∑919 (919)",
		                   "data": {
		                        "$area": 919,
		                        "$dim": 919,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/ldap/pool",
		                   "name": "pool ∑1108 (1108)",
		                   "data": {
		                        "$area": 1108,
		                        "$dim": 1108,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/ldap/ext",
		                   "name": "ext ∑363 (363)",
		                   "data": {
		                        "$area": 363,
		                        "$dim": 363,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/res",
		                   "name": "sun.org.apache.xml.internal.res ∑26846 (15741)",
		                   "data": {
		                        "$area": 26846,
		                        "$dim": 26846,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/resolver",
		                   "name": "lver ∑11105 (5574)",
		                   "data": {
		                        "$area": 11105,
		                        "$dim": 11105,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/resolver/tools",
		                   "name": "tools ∑1484 (1484)",
		                   "data": {
		                        "$area": 1484,
		                        "$dim": 1484,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/resolver/helpers",
		                   "name": "helpers ∑662 (662)",
		                   "data": {
		                        "$area": 662,
		                        "$dim": 662,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/resolver/readers",
		                   "name": "readers ∑3385 (3385)",
		                   "data": {
		                        "$area": 3385,
		                        "$dim": 3385,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/oracle/xmlns/internal/webservices/jaxws_databinding",
		                   "name": "oracle.xmlns.internal.webservices.jaxws_databinding ∑1546 (1546)",
		                   "data": {
		                        "$area": 1546,
		                        "$dim": 1546,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/dtm",
		                   "name": "sun.org.apache.xml.internal.dtm ∑24823 (642)",
		                   "data": {
		                        "$area": 24823,
		                        "$dim": 24823,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/dtm/ref",
		                   "name": "ref ∑24181 (14714)",
		                   "data": {
		                        "$area": 24181,
		                        "$dim": 24181,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/dtm/ref/dom2dtm",
		                   "name": "dom2dtm ∑2013 (2013)",
		                   "data": {
		                        "$area": 2013,
		                        "$dim": 2013,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/dtm/ref/sax2dtm",
		                   "name": "sax2dtm ∑7454 (7454)",
		                   "data": {
		                        "$area": 7454,
		                        "$dim": 7454,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/oracle/net",
		                   "name": "oracle.net ∑159 (159)",
		                   "data": {
		                        "$area": 159,
		                        "$dim": 159,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/toolkit/url",
		                   "name": "sun.jndi.toolkit.url ∑1950 (1950)",
		                   "data": {
		                        "$area": 1950,
		                        "$dim": 1950,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/spi",
		                   "name": "sun.xml.internal.ws.spi ∑3238 (425)",
		                   "data": {
		                        "$area": 3238,
		                        "$dim": 3238,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/spi/db",
		                   "name": "db ∑2813 (2813)",
		                   "data": {
		                        "$area": 2813,
		                        "$dim": 2813,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xpath/internal",
		                   "name": "sun.org.apache.xpath.internal ∑56705 (5483)",
		                   "data": {
		                        "$area": 56705,
		                        "$dim": 56705,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xpath/internal/operations",
		                   "name": "operations ∑828 (828)",
		                   "data": {
		                        "$area": 828,
		                        "$dim": 828,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/patterns",
		                   "name": "patterns ∑2329 (2329)",
		                   "data": {
		                        "$area": 2329,
		                        "$dim": 2329,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/domapi",
		                   "name": "domapi ∑991 (991)",
		                   "data": {
		                        "$area": 991,
		                        "$dim": 991,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/functions",
		                   "name": "functions ∑2602 (2602)",
		                   "data": {
		                        "$area": 2602,
		                        "$dim": 2602,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/res",
		                   "name": "res ∑24234 (24234)",
		                   "data": {
		                        "$area": 24234,
		                        "$dim": 24234,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/jaxp",
		                   "name": "jaxp ∑1677 (1677)",
		                   "data": {
		                        "$area": 1677,
		                        "$dim": 1677,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/compiler",
		                   "name": "compiler ∑6549 (6549)",
		                   "data": {
		                        "$area": 6549,
		                        "$dim": 6549,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/objects",
		                   "name": "objects ∑3801 (3801)",
		                   "data": {
		                        "$area": 3801,
		                        "$dim": 3801,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xpath/internal/axes",
		                   "name": "axes ∑8211 (8211)",
		                   "data": {
		                        "$area": 8211,
		                        "$dim": 8211,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/impl/dynamicany",
		                   "name": "sun.corba.se.impl.dynamicany ∑7384 (7384)",
		                   "data": {
		                        "$area": 7384,
		                        "$dim": 7384,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/xs",
		                   "name": "sun.org.apache.xerces.internal.xs ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security",
		                   "name": "sun.org.apache.xml.internal.security ∑40855 (690)",
		                   "data": {
		                        "$area": 40855,
		                        "$dim": 40855,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/utils",
		                   "name": "utils ∑8622 (6642)",
		                   "data": {
		                        "$area": 8622,
		                        "$dim": 8622,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/utils/resolver",
		                   "name": "resolver ∑1980 (786)",
		                   "data": {
		                        "$area": 1980,
		                        "$dim": 1980,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/utils/resolver/implementations",
		                   "name": "implementations ∑1194 (1194)",
		                   "data": {
		                        "$area": 1194,
		                        "$dim": 1194,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/c14n",
		                   "name": "c14n ∑5780 (329)",
		                   "data": {
		                        "$area": 5780,
		                        "$dim": 5780,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/c14n/helper",
		                   "name": "helper ∑221 (221)",
		                   "data": {
		                        "$area": 221,
		                        "$dim": 221,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security/c14n/implementations",
		                   "name": "implementations ∑5230 (5230)",
		                   "data": {
		                        "$area": 5230,
		                        "$dim": 5230,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/algorithms",
		                   "name": "algorithms ∑3801 (1612)",
		                   "data": {
		                        "$area": 3801,
		                        "$dim": 3801,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/algorithms/implementations",
		                   "name": "implementations ∑2189 (2189)",
		                   "data": {
		                        "$area": 2189,
		                        "$dim": 2189,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/keys",
		                   "name": "keys ∑7874 (1613)",
		                   "data": {
		                        "$area": 7874,
		                        "$dim": 7874,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/keys/storage",
		                   "name": "storage ∑859 (184)",
		                   "data": {
		                        "$area": 859,
		                        "$dim": 859,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/keys/storage/implementations",
		                   "name": "implementations ∑675 (675)",
		                   "data": {
		                        "$area": 675,
		                        "$dim": 675,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/keys/content",
		                   "name": "content ∑1747 (956)",
		                   "data": {
		                        "$area": 1747,
		                        "$dim": 1747,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/keys/content/x509",
		                   "name": "x509 ∑560 (560)",
		                   "data": {
		                        "$area": 560,
		                        "$dim": 560,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security/keys/content/keyvalues",
		                   "name": "keyvalues ∑231 (231)",
		                   "data": {
		                        "$area": 231,
		                        "$dim": 231,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/keys/keyresolver",
		                   "name": "keyresolver ∑3655 (674)",
		                   "data": {
		                        "$area": 3655,
		                        "$dim": 3655,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/keys/keyresolver/implementations",
		                   "name": "implementations ∑2981 (2981)",
		                   "data": {
		                        "$area": 2981,
		                        "$dim": 2981,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/exceptions",
		                   "name": "exceptions ∑341 (341)",
		                   "data": {
		                        "$area": 341,
		                        "$dim": 341,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security/signature",
		                   "name": "signature ∑5107 (4870)",
		                   "data": {
		                        "$area": 5107,
		                        "$dim": 5107,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/signature/reference",
		                   "name": "reference ∑237 (237)",
		                   "data": {
		                        "$area": 237,
		                        "$dim": 237,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/security/encryption",
		                   "name": "encryption ∑5240 (5240)",
		                   "data": {
		                        "$area": 5240,
		                        "$dim": 5240,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security/transforms",
		                   "name": "transforms ∑3400 (1188)",
		                   "data": {
		                        "$area": 3400,
		                        "$dim": 3400,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/security/transforms/params",
		                   "name": "params ∑789 (789)",
		                   "data": {
		                        "$area": 789,
		                        "$dim": 789,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/security/transforms/implementations",
		                   "name": "implementations ∑1423 (1423)",
		                   "data": {
		                        "$area": 1423,
		                        "$dim": 1423,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/xml/internal/ws/config/metro/dev",
		                   "name": "sun.xml.internal.ws.config.metro.dev ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/tracing",
		                   "name": "sun.tracing ∑395 (159)",
		                   "data": {
		                        "$area": 395,
		                        "$dim": 395,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/tracing/dtrace",
		                   "name": "dtrace ∑236 (236)",
		                   "data": {
		                        "$area": 236,
		                        "$dim": 236,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/spi/encoding",
		                   "name": "sun.corba.se.spi.encoding ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/net/httpserver",
		                   "name": "sun.net.httpserver ∑723 (603)",
		                   "data": {
		                        "$area": 723,
		                        "$dim": 723,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/net/httpserver/spi",
		                   "name": "spi ∑120 (120)",
		                   "data": {
		                        "$area": 120,
		                        "$dim": 120,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/messaging/saaj",
		                   "name": "sun.xml.internal.messaging.saaj ∑33278 (108)",
		                   "data": {
		                        "$area": 33278,
		                        "$dim": 33278,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/messaging/saaj/soap",
		                   "name": "soap ∑15818 (6747)",
		                   "data": {
		                        "$area": 15818,
		                        "$dim": 15818,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/messaging/saaj/soap/dynamic",
		                   "name": "dynamic ∑18 (18)",
		                   "data": {
		                        "$area": 18,
		                        "$dim": 18,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/soap/impl",
		                   "name": "impl ∑4995 (4995)",
		                   "data": {
		                        "$area": 4995,
		                        "$dim": 4995,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/soap/ver1_2",
		                   "name": "ver1_2 ∑2053 (2053)",
		                   "data": {
		                        "$area": 2053,
		                        "$dim": 2053,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/soap/ver1_1",
		                   "name": "ver1_1 ∑1278 (1278)",
		                   "data": {
		                        "$area": 1278,
		                        "$dim": 1278,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/soap/name",
		                   "name": "name ∑727 (727)",
		                   "data": {
		                        "$area": 727,
		                        "$dim": 727,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/messaging/saaj/client/p2p",
		                   "name": "client.p2p ∑1246 (1246)",
		                   "data": {
		                        "$area": 1246,
		                        "$dim": 1246,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/packaging/mime",
		                   "name": "packaging.mime ∑10323 (112)",
		                   "data": {
		                        "$area": 10323,
		                        "$dim": 10323,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/messaging/saaj/packaging/mime/util",
		                   "name": "util ∑3076 (3076)",
		                   "data": {
		                        "$area": 3076,
		                        "$dim": 3076,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/messaging/saaj/packaging/mime/internet",
		                   "name": "internet ∑7135 (7135)",
		                   "data": {
		                        "$area": 7135,
		                        "$dim": 7135,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/messaging/saaj/util",
		                   "name": "util ∑5783 (5373)",
		                   "data": {
		                        "$area": 5783,
		                        "$dim": 5783,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/messaging/saaj/util/transform",
		                   "name": "transform ∑410 (410)",
		                   "data": {
		                        "$area": 410,
		                        "$dim": 410,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/jmx/remote/protocol/rmi",
		                   "name": "sun.jmx.remote.protocol.rmi ∑55 (55)",
		                   "data": {
		                        "$area": 55,
		                        "$dim": 55,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/beans",
		                   "name": "sun.beans ∑6891 (598)",
		                   "data": {
		                        "$area": 6891,
		                        "$dim": 6891,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/beans/util",
		                   "name": "util ∑818 (818)",
		                   "data": {
		                        "$area": 818,
		                        "$dim": 818,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/beans/editors",
		                   "name": "editors ∑1715 (1715)",
		                   "data": {
		                        "$area": 1715,
		                        "$dim": 1715,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/beans/finder",
		                   "name": "finder ∑1749 (1749)",
		                   "data": {
		                        "$area": 1749,
		                        "$dim": 1749,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/beans/decoder",
		                   "name": "decoder ∑1905 (1905)",
		                   "data": {
		                        "$area": 1905,
		                        "$dim": 1905,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/beans/infos",
		                   "name": "infos ∑106 (106)",
		                   "data": {
		                        "$area": 106,
		                        "$dim": 106,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/management",
		                   "name": "sun.management ∑1224 (662)",
		                   "data": {
		                        "$area": 1224,
		                        "$dim": 1224,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/management/jmx",
		                   "name": "jmx ∑562 (562)",
		                   "data": {
		                        "$area": 562,
		                        "$dim": 562,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/encoding",
		                   "name": "sun.xml.internal.ws.encoding ∑8405 (6154)",
		                   "data": {
		                        "$area": 8405,
		                        "$dim": 8405,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/encoding/xml",
		                   "name": "xml ∑880 (880)",
		                   "data": {
		                        "$area": 880,
		                        "$dim": 880,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/encoding/policy",
		                   "name": "policy ∑493 (493)",
		                   "data": {
		                        "$area": 493,
		                        "$dim": 493,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/encoding/soap",
		                   "name": "soap ∑308 (302)",
		                   "data": {
		                        "$area": 308,
		                        "$dim": 308,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/encoding/soap/streaming",
		                   "name": "streaming ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/encoding/fastinfoset",
		                   "name": "fastinfoset ∑570 (570)",
		                   "data": {
		                        "$area": 570,
		                        "$dim": 570,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/stream",
		                   "name": "sun.xml.internal.stream ∑22551 (2708)",
		                   "data": {
		                        "$area": 22551,
		                        "$dim": 22551,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/stream/dtd",
		                   "name": "dtd ∑2871 (804)",
		                   "data": {
		                        "$area": 2871,
		                        "$dim": 2871,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/stream/dtd/nonvalidating",
		                   "name": "nonvalidating ∑2067 (2067)",
		                   "data": {
		                        "$area": 2067,
		                        "$dim": 2067,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/stream/util",
		                   "name": "util ∑203 (203)",
		                   "data": {
		                        "$area": 203,
		                        "$dim": 203,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/stream/buffer",
		                   "name": "buffer ∑8133 (1973)",
		                   "data": {
		                        "$area": 8133,
		                        "$dim": 8133,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/stream/buffer/sax",
		                   "name": "sax ∑1696 (1696)",
		                   "data": {
		                        "$area": 1696,
		                        "$dim": 1696,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/stream/buffer/stax",
		                   "name": "stax ∑4464 (4464)",
		                   "data": {
		                        "$area": 4464,
		                        "$dim": 4464,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/stream/events",
		                   "name": "events ∑3005 (3005)",
		                   "data": {
		                        "$area": 3005,
		                        "$dim": 3005,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/stream/writers",
		                   "name": "writers ∑5631 (5631)",
		                   "data": {
		                        "$area": 5631,
		                        "$dim": 5631,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/spi/activation",
		                   "name": "sun.corba.se.spi.activation ∑8800 (7931)",
		                   "data": {
		                        "$area": 8800,
		                        "$dim": 8800,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/spi/activation/RepositoryPackage",
		                   "name": "RepositoryPackage ∑378 (378)",
		                   "data": {
		                        "$area": 378,
		                        "$dim": 378,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/activation/LocatorPackage",
		                   "name": "LocatorPackage ∑362 (362)",
		                   "data": {
		                        "$area": 362,
		                        "$dim": 362,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/activation/InitialNameServicePackage",
		                   "name": "InitialNameServicePackage ∑129 (129)",
		                   "data": {
		                        "$area": 129,
		                        "$dim": 129,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/oracle/webservices/internal/impl/encoding",
		                   "name": "oracle.webservices.internal.impl.encoding ∑20 (20)",
		                   "data": {
		                        "$area": 20,
		                        "$dim": 20,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/resources",
		                   "name": "sun.xml.internal.ws.resources ∑6370 (6370)",
		                   "data": {
		                        "$area": 6370,
		                        "$dim": 6370,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/activation",
		                   "name": "sun.corba.se.impl.activation ∑6097 (6097)",
		                   "data": {
		                        "$area": 6097,
		                        "$dim": 6097,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/fault",
		                   "name": "sun.xml.internal.ws.fault ∑1950 (1950)",
		                   "data": {
		                        "$area": 1950,
		                        "$dim": 1950,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/logging",
		                   "name": "sun.corba.se.impl.logging ∑28488 (28488)",
		                   "data": {
		                        "$area": 28488,
		                        "$dim": 28488,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/internal/corba",
		                   "name": "sun.corba.se.internal.corba ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/config/management/policy",
		                   "name": "sun.xml.internal.ws.config.management.policy ∑99 (99)",
		                   "data": {
		                        "$area": 99,
		                        "$dim": 99,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/message",
		                   "name": "sun.xml.internal.ws.message ∑8303 (2335)",
		                   "data": {
		                        "$area": 8303,
		                        "$dim": 8303,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/message/stream",
		                   "name": "stream ∑2396 (2396)",
		                   "data": {
		                        "$area": 2396,
		                        "$dim": 2396,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/message/jaxb",
		                   "name": "jaxb ∑1568 (1568)",
		                   "data": {
		                        "$area": 1568,
		                        "$dim": 1568,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/message/saaj",
		                   "name": "saaj ∑1516 (1516)",
		                   "data": {
		                        "$area": 1516,
		                        "$dim": 1516,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/message/source",
		                   "name": "source ∑488 (488)",
		                   "data": {
		                        "$area": 488,
		                        "$dim": 488,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/jmx/defaults",
		                   "name": "sun.jmx.defaults ∑37 (37)",
		                   "data": {
		                        "$area": 37,
		                        "$dim": 37,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/xinclude",
		                   "name": "sun.org.apache.xerces.internal.xinclude ∑7433 (7433)",
		                   "data": {
		                        "$area": 7433,
		                        "$dim": 7433,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/util",
		                   "name": "sun.xml.internal.ws.util ∑10161 (5373)",
		                   "data": {
		                        "$area": 10161,
		                        "$dim": 10161,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/util/exception",
		                   "name": "exception ∑313 (313)",
		                   "data": {
		                        "$area": 313,
		                        "$dim": 313,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/util/xml",
		                   "name": "xml ∑2993 (2993)",
		                   "data": {
		                        "$area": 2993,
		                        "$dim": 2993,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/util/pipe",
		                   "name": "pipe ∑1482 (1482)",
		                   "data": {
		                        "$area": 1482,
		                        "$dim": 1482,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/impl/io",
		                   "name": "sun.corba.se.impl.io ∑16395 (11771)",
		                   "data": {
		                        "$area": 16395,
		                        "$dim": 16395,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/ior",
		                   "name": " ∑4624 (3440)",
		                   "data": {
		                        "$area": 4624,
		                        "$dim": 4624,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/ior/iiop",
		                   "name": "iiop ∑1184 (1184)",
		                   "data": {
		                        "$area": 1184,
		                        "$dim": 1184,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/corba/se/impl/legacy/connection",
		                   "name": "sun.corba.se.impl.legacy.connection ∑795 (795)",
		                   "data": {
		                        "$area": 795,
		                        "$dim": 795,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/interceptor",
		                   "name": "sun.jmx.interceptor ∑2961 (2961)",
		                   "data": {
		                        "$area": 2961,
		                        "$dim": 2961,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/ior",
		                   "name": "sun.corba.se.spi.ior ∑826 (292)",
		                   "data": {
		                        "$area": 826,
		                        "$dim": 826,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/spi/ior/iiop",
		                   "name": "iiop ∑534 (534)",
		                   "data": {
		                        "$area": 534,
		                        "$dim": 534,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/omg/CORBA",
		                   "name": "sun.org.omg.CORBA ∑4219 (3584)",
		                   "data": {
		                        "$area": 4219,
		                        "$dim": 4219,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/omg/CORBA/ValueDefPackage",
		                   "name": "ValueDefPackage ∑635 (635)",
		                   "data": {
		                        "$area": 635,
		                        "$dim": 635,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xml/internal/serialize",
		                   "name": "sun.org.apache.xml.internal.serialize ∑38545 (12983)",
		                   "data": {
		                        "$area": 38545,
		                        "$dim": 38545,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/serializer",
		                   "name": " ∑25562 (16524)",
		                   "data": {
		                        "$area": 25562,
		                        "$dim": 25562,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/serializer/utils",
		                   "name": "utils ∑9038 (9038)",
		                   "data": {
		                        "$area": 9038,
		                        "$dim": 9038,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/corba/se/spi/monitoring",
		                   "name": "sun.corba.se.spi.monitoring ∑472 (472)",
		                   "data": {
		                        "$area": 472,
		                        "$dim": 472,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/apple/laf",
		                   "name": "apple.laf ∑78902 (65108)",
		                   "data": {
		                        "$area": 78902,
		                        "$dim": 78902,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/apple/laf/resources",
		                   "name": "resources ∑13794 (13794)",
		                   "data": {
		                        "$area": 13794,
		                        "$dim": 13794,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/streaming",
		                   "name": "sun.xml.internal.ws.streaming ∑2743 (2743)",
		                   "data": {
		                        "$area": 2743,
		                        "$dim": 2743,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xml/internal/utils",
		                   "name": "sun.org.apache.xml.internal.utils ∑26978 (20693)",
		                   "data": {
		                        "$area": 26978,
		                        "$dim": 26978,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xml/internal/utils/res",
		                   "name": "res ∑6285 (6285)",
		                   "data": {
		                        "$area": 6285,
		                        "$dim": 6285,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/security/auth",
		                   "name": "sun.security.auth ∑12471 (1313)",
		                   "data": {
		                        "$area": 12471,
		                        "$dim": 12471,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/security/auth/callback",
		                   "name": "callback ∑957 (957)",
		                   "data": {
		                        "$area": 957,
		                        "$dim": 957,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/auth/module",
		                   "name": "module ∑10175 (10175)",
		                   "data": {
		                        "$area": 10175,
		                        "$dim": 10175,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/auth/login",
		                   "name": "login ∑26 (26)",
		                   "data": {
		                        "$area": 26,
		                        "$dim": 26,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/org/jvnet/staxex",
		                   "name": "sun.xml.internal.org.jvnet.staxex ∑1635 (1635)",
		                   "data": {
		                        "$area": 1635,
		                        "$dim": 1635,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/protocol/xml",
		                   "name": "sun.xml.internal.ws.protocol.xml ∑21 (21)",
		                   "data": {
		                        "$area": 21,
		                        "$dim": 21,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/cert/internal/x509",
		                   "name": "sun.security.cert.internal.x509 ∑202 (202)",
		                   "data": {
		                        "$area": 202,
		                        "$dim": 202,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal",
		                   "name": "sun.org.apache.xalan.internal ∑174446 (103)",
		                   "data": {
		                        "$area": 174446,
		                        "$dim": 174446,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/utils",
		                   "name": "utils ∑2002 (2002)",
		                   "data": {
		                        "$area": 2002,
		                        "$dim": 2002,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal/templates",
		                   "name": "templates ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal/lib",
		                   "name": "lib ∑3600 (3600)",
		                   "data": {
		                        "$area": 3600,
		                        "$dim": 3600,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal/xslt",
		                   "name": "xslt ∑122875 (3966)",
		                   "data": {
		                        "$area": 122875,
		                        "$dim": 122875,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/xsltc",
		                   "name": " ∑118909 (55)",
		                   "data": {
		                        "$area": 118909,
		                        "$dim": 118909,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/xsltc/runtime",
		                   "name": "runtime ∑9377 (8915)",
		                   "data": {
		                        "$area": 9377,
		                        "$dim": 9377,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/xsltc/runtime/output",
		                   "name": "output ∑462 (462)",
		                   "data": {
		                        "$area": 462,
		                        "$dim": 462,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xalan/internal/xsltc/dom",
		                   "name": "dom ∑14158 (14158)",
		                   "data": {
		                        "$area": 14158,
		                        "$dim": 14158,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal/xsltc/compiler",
		                   "name": "compiler ∑83057 (54384)",
		                   "data": {
		                        "$area": 83057,
		                        "$dim": 83057,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/xsltc/compiler/util",
		                   "name": "util ∑28673 (28673)",
		                   "data": {
		                        "$area": 28673,
		                        "$dim": 28673,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xalan/internal/xsltc/util",
		                   "name": "util ∑458 (458)",
		                   "data": {
		                        "$area": 458,
		                        "$dim": 458,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xalan/internal/xsltc/cmdline",
		                   "name": "cmdline ∑1235 (823)",
		                   "data": {
		                        "$area": 1235,
		                        "$dim": 1235,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xalan/internal/xsltc/cmdline/getopt",
		                   "name": "getopt ∑412 (412)",
		                   "data": {
		                        "$area": 412,
		                        "$dim": 412,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xalan/internal/xsltc/trax",
		                   "name": "trax ∑10569 (10569)",
		                   "data": {
		                        "$area": 10569,
		                        "$dim": 10569,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/org/apache/xalan/internal/res",
		                   "name": "res ∑45863 (45863)",
		                   "data": {
		                        "$area": 45863,
		                        "$dim": 45863,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/imageio/plugins/jpeg",
		                   "name": "sun.imageio.plugins.jpeg ∑21925 (21925)",
		                   "data": {
		                        "$area": 21925,
		                        "$dim": 21925,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/imageio/plugins/bmp",
		                   "name": "sun.imageio.plugins.bmp ∑10498 (10498)",
		                   "data": {
		                        "$area": 10498,
		                        "$dim": 10498,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/naming/namingutil",
		                   "name": "sun.corba.se.impl.naming.namingutil ∑754 (754)",
		                   "data": {
		                        "$area": 754,
		                        "$dim": 754,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/developer",
		                   "name": "sun.xml.internal.ws.developer ∑789 (789)",
		                   "data": {
		                        "$area": 789,
		                        "$dim": 789,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/txw2",
		                   "name": "sun.xml.internal.txw2 ∑5950 (2941)",
		                   "data": {
		                        "$area": 5950,
		                        "$dim": 5950,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/txw2/output",
		                   "name": "output ∑3009 (3009)",
		                   "data": {
		                        "$area": 3009,
		                        "$dim": 3009,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/org/jvnet/mimepull",
		                   "name": "sun.xml.internal.org.jvnet.mimepull ∑6516 (6516)",
		                   "data": {
		                        "$area": 6516,
		                        "$dim": 6516,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/servicecontext",
		                   "name": "sun.corba.se.spi.servicecontext ∑1227 (1227)",
		                   "data": {
		                        "$area": 1227,
		                        "$dim": 1227,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/nio/sctp",
		                   "name": "sun.nio.sctp ∑525 (525)",
		                   "data": {
		                        "$area": 525,
		                        "$dim": 525,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/copyobject",
		                   "name": "sun.corba.se.impl.copyobject ∑155 (155)",
		                   "data": {
		                        "$area": 155,
		                        "$dim": 155,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/client",
		                   "name": "sun.xml.internal.ws.client ∑10725 (4653)",
		                   "data": {
		                        "$area": 10725,
		                        "$dim": 10725,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/client/dispatch",
		                   "name": "dispatch ∑1987 (1987)",
		                   "data": {
		                        "$area": 1987,
		                        "$dim": 1987,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/client/sei",
		                   "name": "sei ∑4085 (4085)",
		                   "data": {
		                        "$area": 4085,
		                        "$dim": 4085,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/jmx/mbeanserver",
		                   "name": "sun.jmx.mbeanserver ∑11948 (11948)",
		                   "data": {
		                        "$area": 11948,
		                        "$dim": 11948,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/oracle/webservices/internal/api",
		                   "name": "oracle.webservices.internal.api ∑1876 (78)",
		                   "data": {
		                        "$area": 1876,
		                        "$dim": 1876,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/oracle/webservices/internal/api/message",
		                   "name": "message ∑1490 (1490)",
		                   "data": {
		                        "$area": 1490,
		                        "$dim": 1490,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/oracle/webservices/internal/api/databinding",
		                   "name": "databinding ∑308 (308)",
		                   "data": {
		                        "$area": 308,
		                        "$dim": 308,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/nio/file",
		                   "name": "sun.nio.file ∑161 (161)",
		                   "data": {
		                        "$area": 161,
		                        "$dim": 161,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/imageio/plugins/gif",
		                   "name": "sun.imageio.plugins.gif ∑9290 (9290)",
		                   "data": {
		                        "$area": 9290,
		                        "$dim": 9290,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/binding",
		                   "name": "sun.xml.internal.ws.binding ∑2057 (2057)",
		                   "data": {
		                        "$area": 2057,
		                        "$dim": 2057,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/java/browser/dom",
		                   "name": "sun.java.browser.dom ∑89 (89)",
		                   "data": {
		                        "$area": 89,
		                        "$dim": 89,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp",
		                   "name": "sun.jmx.snmp ∑45554 (10300)",
		                   "data": {
		                        "$area": 45554,
		                        "$dim": 45554,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/jmx/snmp/daemon",
		                   "name": "daemon ∑12552 (12552)",
		                   "data": {
		                        "$area": 12552,
		                        "$dim": 12552,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp/tasks",
		                   "name": "tasks ∑445 (445)",
		                   "data": {
		                        "$area": 445,
		                        "$dim": 445,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp/IPAcl",
		                   "name": "IPAcl ∑13067 (13067)",
		                   "data": {
		                        "$area": 13067,
		                        "$dim": 13067,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp/defaults",
		                   "name": "defaults ∑237 (237)",
		                   "data": {
		                        "$area": 237,
		                        "$dim": 237,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp/internal",
		                   "name": "internal ∑944 (944)",
		                   "data": {
		                        "$area": 944,
		                        "$dim": 944,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/snmp/agent",
		                   "name": "agent ∑8009 (8009)",
		                   "data": {
		                        "$area": 8009,
		                        "$dim": 8009,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/swing/internal/plaf/synth/resources",
		                   "name": "sun.swing.internal.plaf.synth.resources ∑3972 (3972)",
		                   "data": {
		                        "$area": 3972,
		                        "$dim": 3972,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/oracle/util",
		                   "name": "oracle.util ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/ntlm",
		                   "name": "sun.security.ntlm ∑2169 (2169)",
		                   "data": {
		                        "$area": 2169,
		                        "$dim": 2169,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/oracle/nio",
		                   "name": "oracle.nio ∑90 (90)",
		                   "data": {
		                        "$area": 90,
		                        "$dim": 90,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/omg/SendingContext",
		                   "name": "sun.org.omg.SendingContext ∑844 (608)",
		                   "data": {
		                        "$area": 844,
		                        "$dim": 844,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/omg/SendingContext/CodeBasePackage",
		                   "name": "CodeBasePackage ∑236 (236)",
		                   "data": {
		                        "$area": 236,
		                        "$dim": 236,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/apple/eawt",
		                   "name": "apple.eawt ∑3316 (2818)",
		                   "data": {
		                        "$area": 3316,
		                        "$dim": 3316,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/apple/eawt/event",
		                   "name": "event ∑498 (498)",
		                   "data": {
		                        "$area": 498,
		                        "$dim": 498,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/rmi/rmid",
		                   "name": "sun.rmi.rmid ∑468 (468)",
		                   "data": {
		                        "$area": 468,
		                        "$dim": 468,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/toolkit/corba",
		                   "name": "sun.jndi.toolkit.corba ∑317 (317)",
		                   "data": {
		                        "$area": 317,
		                        "$dim": 317,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/assembler",
		                   "name": "sun.xml.internal.ws.assembler ∑2128 (1878)",
		                   "data": {
		                        "$area": 2128,
		                        "$dim": 2128,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/assembler/jaxws",
		                   "name": "jaxws ∑92 (92)",
		                   "data": {
		                        "$area": 92,
		                        "$dim": 92,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/assembler/dev",
		                   "name": "dev ∑158 (158)",
		                   "data": {
		                        "$area": 158,
		                        "$dim": 158,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/impl/naming/cosnaming",
		                   "name": "sun.corba.se.impl.naming.cosnaming ∑2939 (2939)",
		                   "data": {
		                        "$area": 2939,
		                        "$dim": 2939,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/server",
		                   "name": "sun.xml.internal.ws.server ∑9622 (5798)",
		                   "data": {
		                        "$area": 9622,
		                        "$dim": 9622,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/server/provider",
		                   "name": "provider ∑1033 (1033)",
		                   "data": {
		                        "$area": 1033,
		                        "$dim": 1033,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/server/sei",
		                   "name": "sei ∑2791 (2791)",
		                   "data": {
		                        "$area": 2791,
		                        "$dim": 2791,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/fastinfoset",
		                   "name": "sun.xml.internal.fastinfoset ∑48315 (12317)",
		                   "data": {
		                        "$area": 48315,
		                        "$dim": 48315,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/fastinfoset/alphabet",
		                   "name": "alphabet ∑17 (17)",
		                   "data": {
		                        "$area": 17,
		                        "$dim": 17,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/tools",
		                   "name": "tools ∑2820 (2820)",
		                   "data": {
		                        "$area": 2820,
		                        "$dim": 2820,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/util",
		                   "name": "util ∑5098 (5098)",
		                   "data": {
		                        "$area": 5098,
		                        "$dim": 5098,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/sax",
		                   "name": "sax ∑6380 (6380)",
		                   "data": {
		                        "$area": 6380,
		                        "$dim": 6380,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/dom",
		                   "name": "dom ∑2997 (2997)",
		                   "data": {
		                        "$area": 2997,
		                        "$dim": 2997,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/stax",
		                   "name": "stax ∑9530 (5788)",
		                   "data": {
		                        "$area": 9530,
		                        "$dim": 9530,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/fastinfoset/stax/events",
		                   "name": "events ∑2596 (2596)",
		                   "data": {
		                        "$area": 2596,
		                        "$dim": 2596,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/stax/factory",
		                   "name": "factory ∑886 (886)",
		                   "data": {
		                        "$area": 886,
		                        "$dim": 886,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/stax/util",
		                   "name": "util ∑260 (260)",
		                   "data": {
		                        "$area": 260,
		                        "$dim": 260,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/fastinfoset/algorithm",
		                   "name": "algorithm ∑4785 (4785)",
		                   "data": {
		                        "$area": 4785,
		                        "$dim": 4785,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/org/apache/xerces/util",
		                   "name": "org.apache.xerces.util ∑3288 (3288)",
		                   "data": {
		                        "$area": 3288,
		                        "$dim": 3288,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/fastinfoset/vocab",
		                   "name": "vocab ∑1083 (1083)",
		                   "data": {
		                        "$area": 1083,
		                        "$dim": 1083,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/jndi/url/dns",
		                   "name": "sun.jndi.url.dns ∑133 (133)",
		                   "data": {
		                        "$area": 133,
		                        "$dim": 133,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/glassfish/external/arc",
		                   "name": "sun.org.glassfish.external.arc ∑101 (101)",
		                   "data": {
		                        "$area": 101,
		                        "$dim": 101,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/internal/Interceptors",
		                   "name": "sun.corba.se.internal.Interceptors ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/apple/concurrent",
		                   "name": "apple.concurrent ∑434 (434)",
		                   "data": {
		                        "$area": 434,
		                        "$dim": 434,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/istack/internal",
		                   "name": "sun.istack.internal ∑1740 (722)",
		                   "data": {
		                        "$area": 1740,
		                        "$dim": 1740,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/istack/internal/localization",
		                   "name": "localization ∑263 (263)",
		                   "data": {
		                        "$area": 263,
		                        "$dim": 263,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/istack/internal/logging",
		                   "name": "logging ∑755 (755)",
		                   "data": {
		                        "$area": 755,
		                        "$dim": 755,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/model",
		                   "name": "sun.xml.internal.ws.model ∑13218 (10496)",
		                   "data": {
		                        "$area": 13218,
		                        "$dim": 13218,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/model/soap",
		                   "name": "soap ∑34 (34)",
		                   "data": {
		                        "$area": 34,
		                        "$dim": 34,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/model/wsdl",
		                   "name": "wsdl ∑2688 (2688)",
		                   "data": {
		                        "$area": 2688,
		                        "$dim": 2688,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/bcel/internal",
		                   "name": "sun.org.apache.bcel.internal ∑43761 (6253)",
		                   "data": {
		                        "$area": 43761,
		                        "$dim": 43761,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/bcel/internal/generic",
		                   "name": "generic ∑18557 (18557)",
		                   "data": {
		                        "$area": 18557,
		                        "$dim": 18557,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/bcel/internal/classfile",
		                   "name": "classfile ∑10843 (10843)",
		                   "data": {
		                        "$area": 10843,
		                        "$dim": 10843,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/bcel/internal/util",
		                   "name": "util ∑8108 (8108)",
		                   "data": {
		                        "$area": 8108,
		                        "$dim": 8108,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/xpointer",
		                   "name": "sun.org.apache.xerces.internal.xpointer ∑3990 (3990)",
		                   "data": {
		                        "$area": 3990,
		                        "$dim": 3990,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl",
		                   "name": "sun.org.apache.xerces.internal.impl ∑155303 (34430)",
		                   "data": {
		                        "$area": 155303,
		                        "$dim": 155303,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/impl/io",
		                   "name": "io ∑2015 (2015)",
		                   "data": {
		                        "$area": 2015,
		                        "$dim": 2015,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xs",
		                   "name": "xs ∑61207 (23878)",
		                   "data": {
		                        "$area": 61207,
		                        "$dim": 61207,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/impl/xs/identity",
		                   "name": "identity ∑1569 (1569)",
		                   "data": {
		                        "$area": 1569,
		                        "$dim": 1569,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xs/models",
		                   "name": "models ∑3532 (3532)",
		                   "data": {
		                        "$area": 3532,
		                        "$dim": 3532,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xs/util",
		                   "name": "util ∑1924 (1924)",
		                   "data": {
		                        "$area": 1924,
		                        "$dim": 1924,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xs/traversers",
		                   "name": "traversers ∑26183 (26183)",
		                   "data": {
		                        "$area": 26183,
		                        "$dim": 26183,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xs/opti",
		                   "name": "opti ∑4121 (4121)",
		                   "data": {
		                        "$area": 4121,
		                        "$dim": 4121,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/impl/msg",
		                   "name": "msg ∑1298 (1298)",
		                   "data": {
		                        "$area": 1298,
		                        "$dim": 1298,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/validation",
		                   "name": "validation ∑289 (289)",
		                   "data": {
		                        "$area": 289,
		                        "$dim": 289,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/xpath",
		                   "name": "xpath ∑20044 (4026)",
		                   "data": {
		                        "$area": 20044,
		                        "$dim": 20044,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/impl/xpath/regex",
		                   "name": "regex ∑16018 (16018)",
		                   "data": {
		                        "$area": 16018,
		                        "$dim": 16018,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/impl/dtd",
		                   "name": "dtd ∑14952 (12506)",
		                   "data": {
		                        "$area": 14952,
		                        "$dim": 14952,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/impl/dtd/models",
		                   "name": "models ∑2446 (2446)",
		                   "data": {
		                        "$area": 2446,
		                        "$dim": 2446,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/impl/dv",
		                   "name": "dv ∑21068 (217)",
		                   "data": {
		                        "$area": 21068,
		                        "$dim": 21068,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/impl/dv/util",
		                   "name": "util ∑1180 (1180)",
		                   "data": {
		                        "$area": 1180,
		                        "$dim": 1180,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/dv/xs",
		                   "name": "xs ∑19213 (19213)",
		                   "data": {
		                        "$area": 19213,
		                        "$dim": 19213,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/impl/dv/dtd",
		                   "name": "dtd ∑458 (458)",
		                   "data": {
		                        "$area": 458,
		                        "$dim": 458,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/demo/jvmti/hprof",
		                   "name": "sun.demo.jvmti.hprof ∑74 (74)",
		                   "data": {
		                        "$area": 74,
		                        "$dim": 74,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/swing/internal/plaf/metal/resources",
		                   "name": "sun.swing.internal.plaf.metal.resources ∑4908 (4908)",
		                   "data": {
		                        "$area": 4908,
		                        "$dim": 4908,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/orb",
		                   "name": "sun.corba.se.impl.orb ∑17340 (7736)",
		                   "data": {
		                        "$area": 17340,
		                        "$dim": 17340,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/orbutil",
		                   "name": "til ∑9604 (6108)",
		                   "data": {
		                        "$area": 9604,
		                        "$dim": 9604,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/orbutil/closure",
		                   "name": "closure ∑35 (35)",
		                   "data": {
		                        "$area": 35,
		                        "$dim": 35,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/orbutil/graph",
		                   "name": "graph ∑238 (238)",
		                   "data": {
		                        "$area": 238,
		                        "$dim": 238,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/orbutil/concurrent",
		                   "name": "concurrent ∑1188 (1188)",
		                   "data": {
		                        "$area": 1188,
		                        "$dim": 1188,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/orbutil/threadpool",
		                   "name": "threadpool ∑1364 (1364)",
		                   "data": {
		                        "$area": 1364,
		                        "$dim": 1364,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/orbutil/fsm",
		                   "name": "fsm ∑671 (671)",
		                   "data": {
		                        "$area": 671,
		                        "$dim": 671,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/xml/internal/ws/handler",
		                   "name": "sun.xml.internal.ws.handler ∑4457 (4457)",
		                   "data": {
		                        "$area": 4457,
		                        "$dim": 4457,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/accessibility/internal/resources",
		                   "name": "sun.accessibility.internal.resources ∑16627 (16627)",
		                   "data": {
		                        "$area": 16627,
		                        "$dim": 16627,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/legacy/connection",
		                   "name": "sun.corba.se.spi.legacy.connection ∑9 (9)",
		                   "data": {
		                        "$area": 9,
		                        "$dim": 9,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/glassfish/gmbal",
		                   "name": "sun.org.glassfish.gmbal ∑922 (775)",
		                   "data": {
		                        "$area": 922,
		                        "$dim": 922,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/glassfish/gmbal/util",
		                   "name": "util ∑147 (147)",
		                   "data": {
		                        "$area": 147,
		                        "$dim": 147,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/config/metro/util",
		                   "name": "sun.xml.internal.ws.config.metro.util ∑35 (35)",
		                   "data": {
		                        "$area": 35,
		                        "$dim": 35,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/dns",
		                   "name": "sun.jndi.dns ∑6659 (6659)",
		                   "data": {
		                        "$area": 6659,
		                        "$dim": 6659,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/interceptors",
		                   "name": "sun.corba.se.impl.interceptors ∑6463 (6463)",
		                   "data": {
		                        "$area": 6463,
		                        "$dim": 6463,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/xni",
		                   "name": "sun.org.apache.xerces.internal.xni ∑738 (398)",
		                   "data": {
		                        "$area": 738,
		                        "$dim": 738,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/xni/parser",
		                   "name": "parser ∑340 (340)",
		                   "data": {
		                        "$area": 340,
		                        "$dim": 340,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/commons/xmlutil",
		                   "name": "sun.xml.internal.ws.commons.xmlutil ∑281 (281)",
		                   "data": {
		                        "$area": 281,
		                        "$dim": 281,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/url/iiop",
		                   "name": "sun.jndi.url.iiop ∑140 (137)",
		                   "data": {
		                        "$area": 140,
		                        "$dim": 140,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/jndi/url/iiopname",
		                   "name": "ame ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/util",
		                   "name": "sun.org.apache.xerces.internal.util ∑22971 (20731)",
		                   "data": {
		                        "$area": 22971,
		                        "$dim": 22971,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/utils",
		                   "name": " ∑2240 (2240)",
		                   "data": {
		                        "$area": 2240,
		                        "$dim": 2240,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/internal/POA",
		                   "name": "sun.corba.se.internal.POA ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/java/browser/net",
		                   "name": "sun.java.browser.net ∑30 (30)",
		                   "data": {
		                        "$area": 30,
		                        "$dim": 30,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/presentation/rmi",
		                   "name": "sun.corba.se.impl.presentation.rmi ∑5414 (5414)",
		                   "data": {
		                        "$area": 5414,
		                        "$dim": 5414,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind",
		                   "name": "sun.xml.internal.bind ∑63805 (2365)",
		                   "data": {
		                        "$area": 63805,
		                        "$dim": 63805,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/bind/marshaller",
		                   "name": "marshaller ∑1341 (1341)",
		                   "data": {
		                        "$area": 1341,
		                        "$dim": 1341,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/api",
		                   "name": "api ∑1770 (749)",
		                   "data": {
		                        "$area": 1770,
		                        "$dim": 1770,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/bind/api/impl",
		                   "name": "impl ∑1021 (1021)",
		                   "data": {
		                        "$area": 1021,
		                        "$dim": 1021,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/bind/util",
		                   "name": "util ∑966 (966)",
		                   "data": {
		                        "$area": 966,
		                        "$dim": 966,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/unmarshaller",
		                   "name": "unmarshaller ∑493 (493)",
		                   "data": {
		                        "$area": 493,
		                        "$dim": 493,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2",
		                   "name": "v2 ∑56870 (1043)",
		                   "data": {
		                        "$area": 56870,
		                        "$dim": 56870,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/bind/v2/model/annotation",
		                   "name": "model.annotation ∑1268 (1268)",
		                   "data": {
		                        "$area": 1268,
		                        "$dim": 1268,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/model/core",
		                   "name": "model.core ∑346 (346)",
		                   "data": {
		                        "$area": 346,
		                        "$dim": 346,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/model/runtime",
		                   "name": "model.runtime ∑73 (73)",
		                   "data": {
		                        "$area": 73,
		                        "$dim": 73,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/util",
		                   "name": "util ∑2079 (2079)",
		                   "data": {
		                        "$area": 2079,
		                        "$dim": 2079,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/model/impl",
		                   "name": "model.impl ∑13523 (13523)",
		                   "data": {
		                        "$area": 13523,
		                        "$dim": 13523,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/runtime",
		                   "name": "runtime ∑32187 (10657)",
		                   "data": {
		                        "$area": 32187,
		                        "$dim": 32187,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/bind/v2/runtime/reflect",
		                   "name": "reflect ∑5984 (3598)",
		                   "data": {
		                        "$area": 5984,
		                        "$dim": 5984,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/bind/v2/runtime/reflect/opt",
		                   "name": "opt ∑2386 (2386)",
		                   "data": {
		                        "$area": 2386,
		                        "$dim": 2386,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/bind/v2/runtime/output",
		                   "name": "output ∑4738 (4738)",
		                   "data": {
		                        "$area": 4738,
		                        "$dim": 4738,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/runtime/property",
		                   "name": "property ∑3166 (3166)",
		                   "data": {
		                        "$area": 3166,
		                        "$dim": 3166,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/runtime/unmarshaller",
		                   "name": "unmarshaller ∑7642 (7642)",
		                   "data": {
		                        "$area": 7642,
		                        "$dim": 7642,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/bind/v2/bytecode",
		                   "name": "bytecode ∑329 (329)",
		                   "data": {
		                        "$area": 329,
		                        "$dim": 329,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/model/nav",
		                   "name": "model.nav ∑1792 (1792)",
		                   "data": {
		                        "$area": 1792,
		                        "$dim": 1792,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/schemagen",
		                   "name": "schemagen ∑4200 (4200)",
		                   "data": {
		                        "$area": 4200,
		                        "$dim": 4200,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/bind/v2/model/util",
		                   "name": "model.util ∑30 (30)",
		                   "data": {
		                        "$area": 30,
		                        "$dim": 30,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/xml/internal/ws/db",
		                   "name": "sun.xml.internal.ws.db ∑1898 (943)",
		                   "data": {
		                        "$area": 1898,
		                        "$dim": 1898,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/db/glassfish",
		                   "name": "glassfish ∑955 (955)",
		                   "data": {
		                        "$area": 955,
		                        "$dim": 955,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/jaxp",
		                   "name": "sun.org.apache.xerces.internal.jaxp ∑17751 (3699)",
		                   "data": {
		                        "$area": 17751,
		                        "$dim": 17751,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/jaxp/validation",
		                   "name": "validation ∑7146 (7146)",
		                   "data": {
		                        "$area": 7146,
		                        "$dim": 7146,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/xerces/internal/jaxp/datatype",
		                   "name": "datatype ∑6906 (6906)",
		                   "data": {
		                        "$area": 6906,
		                        "$dim": 6906,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/spi/legacy/interceptor",
		                   "name": "sun.corba.se.spi.legacy.interceptor ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/orb",
		                   "name": "sun.corba.se.spi.orb ∑2806 (1922)",
		                   "data": {
		                        "$area": 2806,
		                        "$dim": 2806,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/spi/orbutil/threadpool",
		                   "name": "til.threadpool ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/orbutil/closure",
		                   "name": "til.closure ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/orbutil/fsm",
		                   "name": "til.fsm ∑750 (750)",
		                   "data": {
		                        "$area": 750,
		                        "$dim": 750,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/orbutil/proxy",
		                   "name": "til.proxy ∑115 (115)",
		                   "data": {
		                        "$area": 115,
		                        "$dim": 115,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/jmx/remote/util",
		                   "name": "sun.jmx.remote.util ∑1385 (1385)",
		                   "data": {
		                        "$area": 1385,
		                        "$dim": 1385,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/rowset",
		                   "name": "sun.rowset ∑23043 (14710)",
		                   "data": {
		                        "$area": 23043,
		                        "$dim": 23043,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/rowset/providers",
		                   "name": "providers ∑160 (160)",
		                   "data": {
		                        "$area": 160,
		                        "$dim": 160,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/rowset/internal",
		                   "name": "internal ∑8173 (8173)",
		                   "data": {
		                        "$area": 8173,
		                        "$dim": 8173,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/imageio/plugins/common",
		                   "name": "sun.imageio.plugins.common ∑6924 (6924)",
		                   "data": {
		                        "$area": 6924,
		                        "$dim": 6924,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/toolkit/dir",
		                   "name": "sun.jndi.toolkit.dir ∑3342 (3342)",
		                   "data": {
		                        "$area": 3342,
		                        "$dim": 3342,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/apache/regexp/internal",
		                   "name": "sun.org.apache.regexp.internal ∑7428 (7428)",
		                   "data": {
		                        "$area": 7428,
		                        "$dim": 7428,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/transport",
		                   "name": "sun.corba.se.impl.transport ∑8327 (8327)",
		                   "data": {
		                        "$area": 8327,
		                        "$dim": 8327,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/activation/registries",
		                   "name": "sun.activation.registries ∑1988 (1988)",
		                   "data": {
		                        "$area": 1988,
		                        "$dim": 1988,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/remote/security",
		                   "name": "sun.jmx.remote.security ∑2573 (2573)",
		                   "data": {
		                        "$area": 2573,
		                        "$dim": 2573,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/apple/eio",
		                   "name": "apple.eio ∑284 (284)",
		                   "data": {
		                        "$area": 284,
		                        "$dim": 284,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/addressing",
		                   "name": "sun.xml.internal.ws.addressing ∑5447 (4330)",
		                   "data": {
		                        "$area": 5447,
		                        "$dim": 5447,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/addressing/v200408",
		                   "name": "v200408 ∑393 (393)",
		                   "data": {
		                        "$area": 393,
		                        "$dim": 393,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/addressing/policy",
		                   "name": "policy ∑673 (673)",
		                   "data": {
		                        "$area": 673,
		                        "$dim": 673,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/addressing/model",
		                   "name": "model ∑51 (51)",
		                   "data": {
		                        "$area": 51,
		                        "$dim": 51,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/imageio/plugins/png",
		                   "name": "sun.imageio.plugins.png ∑14823 (14823)",
		                   "data": {
		                        "$area": 14823,
		                        "$dim": 14823,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/swing/internal/plaf/basic/resources",
		                   "name": "sun.swing.internal.plaf.basic.resources ∑16452 (16452)",
		                   "data": {
		                        "$area": 16452,
		                        "$dim": 16452,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/resolver",
		                   "name": "sun.corba.se.impl.resolver ∑914 (914)",
		                   "data": {
		                        "$area": 914,
		                        "$dim": 914,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/javax/rmi",
		                   "name": "sun.corba.se.impl.javax.rmi ∑1370 (307)",
		                   "data": {
		                        "$area": 1370,
		                        "$dim": 1370,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/javax/rmi/CORBA",
		                   "name": "CORBA ∑1063 (1063)",
		                   "data": {
		                        "$area": 1063,
		                        "$dim": 1063,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/spi/resolver",
		                   "name": "sun.corba.se.spi.resolver ∑50 (50)",
		                   "data": {
		                        "$area": 50,
		                        "$dim": 50,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/naming/internal",
		                   "name": "sun.naming.internal ∑1441 (1441)",
		                   "data": {
		                        "$area": 1441,
		                        "$dim": 1441,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/glassfish/external/probe/provider",
		                   "name": "sun.org.glassfish.external.probe.provider ∑229 (229)",
		                   "data": {
		                        "$area": 229,
		                        "$dim": 229,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/sasl",
		                   "name": "sun.security.sasl ∑10016 (1396)",
		                   "data": {
		                        "$area": 10016,
		                        "$dim": 10016,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/security/sasl/gsskerb",
		                   "name": "gsskerb ∑1297 (1297)",
		                   "data": {
		                        "$area": 1297,
		                        "$dim": 1297,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/sasl/ntlm",
		                   "name": "ntlm ∑640 (640)",
		                   "data": {
		                        "$area": 640,
		                        "$dim": 640,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/sasl/digest",
		                   "name": "digest ∑5855 (5855)",
		                   "data": {
		                        "$area": 5855,
		                        "$dim": 5855,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/sasl/util",
		                   "name": "util ∑828 (828)",
		                   "data": {
		                        "$area": 828,
		                        "$dim": 828,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/spi/copyobject",
		                   "name": "sun.corba.se.spi.copyobject ∑96 (96)",
		                   "data": {
		                        "$area": 96,
		                        "$dim": 96,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/dump",
		                   "name": "sun.xml.internal.ws.dump ∑678 (678)",
		                   "data": {
		                        "$area": 678,
		                        "$dim": 678,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/protocol/soap",
		                   "name": "sun.xml.internal.ws.protocol.soap ∑371 (371)",
		                   "data": {
		                        "$area": 371,
		                        "$dim": 371,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/toolkit/ctx",
		                   "name": "sun.jndi.toolkit.ctx ∑4389 (4389)",
		                   "data": {
		                        "$area": 4389,
		                        "$dim": 4389,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/protocol",
		                   "name": "sun.corba.se.spi.protocol ∑230 (230)",
		                   "data": {
		                        "$area": 230,
		                        "$dim": 230,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/org/omg/CORBA",
		                   "name": "sun.corba.se.org.omg.CORBA ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/transport",
		                   "name": "sun.corba.se.spi.transport ∑57 (57)",
		                   "data": {
		                        "$area": 57,
		                        "$dim": 57,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/extension",
		                   "name": "sun.corba.se.spi.extension ∑196 (196)",
		                   "data": {
		                        "$area": 196,
		                        "$dim": 196,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/presentation/rmi",
		                   "name": "sun.corba.se.spi.presentation.rmi ∑253 (253)",
		                   "data": {
		                        "$area": 253,
		                        "$dim": 253,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/imageio/stream",
		                   "name": "sun.imageio.stream ∑235 (235)",
		                   "data": {
		                        "$area": 235,
		                        "$dim": 235,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/awt",
		                   "name": "sun.awt ∑353 (353)",
		                   "data": {
		                        "$area": 353,
		                        "$dim": 353,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/remote/protocol/iiop",
		                   "name": "sun.jmx.remote.protocol.iiop ∑417 (417)",
		                   "data": {
		                        "$area": 417,
		                        "$dim": 417,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/oa",
		                   "name": "sun.corba.se.impl.oa ∑8045 (9)",
		                   "data": {
		                        "$area": 8045,
		                        "$dim": 8045,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/oa/poa",
		                   "name": "poa ∑7333 (7333)",
		                   "data": {
		                        "$area": 7333,
		                        "$dim": 7333,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/oa/toa",
		                   "name": "toa ∑703 (703)",
		                   "data": {
		                        "$area": 703,
		                        "$dim": 703,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/java_cup/internal/runtime",
		                   "name": "sun.java_cup.internal.runtime ∑1745 (1745)",
		                   "data": {
		                        "$area": 1745,
		                        "$dim": 1745,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api",
		                   "name": "sun.xml.internal.ws.api ∑22016 (1443)",
		                   "data": {
		                        "$area": 22016,
		                        "$dim": 22016,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/model",
		                   "name": "model ∑526 (253)",
		                   "data": {
		                        "$area": 526,
		                        "$dim": 526,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/model/soap",
		                   "name": "soap ∑51 (51)",
		                   "data": {
		                        "$area": 51,
		                        "$dim": 51,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/model/wsdl",
		                   "name": "wsdl ∑222 (117)",
		                   "data": {
		                        "$area": 222,
		                        "$dim": 222,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/model/wsdl/editable",
		                   "name": "editable ∑105 (105)",
		                   "data": {
		                        "$area": 105,
		                        "$dim": 105,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/xml/internal/ws/api/client",
		                   "name": "client ∑207 (207)",
		                   "data": {
		                        "$area": 207,
		                        "$dim": 207,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/policy",
		                   "name": "policy ∑723 (281)",
		                   "data": {
		                        "$area": 723,
		                        "$dim": 723,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/policy/subject",
		                   "name": "subject ∑442 (442)",
		                   "data": {
		                        "$area": 442,
		                        "$dim": 442,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/api/pipe",
		                   "name": "pipe ∑4482 (4146)",
		                   "data": {
		                        "$area": 4482,
		                        "$dim": 4482,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/pipe/helper",
		                   "name": "helper ∑336 (336)",
		                   "data": {
		                        "$area": 336,
		                        "$dim": 336,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/api/config/management",
		                   "name": "config.management ∑893 (27)",
		                   "data": {
		                        "$area": 893,
		                        "$dim": 893,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/config/management/policy",
		                   "name": "policy ∑866 (866)",
		                   "data": {
		                        "$area": 866,
		                        "$dim": 866,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/api/message",
		                   "name": "message ∑6375 (4715)",
		                   "data": {
		                        "$area": 6375,
		                        "$dim": 6375,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/api/message/stream",
		                   "name": "stream ∑56 (56)",
		                   "data": {
		                        "$area": 56,
		                        "$dim": 56,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/message/saaj",
		                   "name": "saaj ∑1604 (1604)",
		                   "data": {
		                        "$area": 1604,
		                        "$dim": 1604,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/api/ha",
		                   "name": "ha ∑21 (21)",
		                   "data": {
		                        "$area": 21,
		                        "$dim": 21,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/wsdl/writer",
		                   "name": "wsdl.writer ∑55 (55)",
		                   "data": {
		                        "$area": 55,
		                        "$dim": 55,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/databinding",
		                   "name": "databinding ∑385 (385)",
		                   "data": {
		                        "$area": 385,
		                        "$dim": 385,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/wsdl/parser",
		                   "name": "wsdl.parser ∑98 (98)",
		                   "data": {
		                        "$area": 98,
		                        "$dim": 98,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/server",
		                   "name": "server ∑1651 (1651)",
		                   "data": {
		                        "$area": 1651,
		                        "$dim": 1651,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/streaming",
		                   "name": "streaming ∑1332 (1332)",
		                   "data": {
		                        "$area": 1332,
		                        "$dim": 1332,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/fastinfoset",
		                   "name": "fastinfoset ∑14 (14)",
		                   "data": {
		                        "$area": 14,
		                        "$dim": 14,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/api/addressing",
		                   "name": "addressing ∑3811 (3811)",
		                   "data": {
		                        "$area": 3811,
		                        "$dim": 3811,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/dom",
		                   "name": "sun.org.apache.xerces.internal.dom ∑30294 (30177)",
		                   "data": {
		                        "$area": 30294,
		                        "$dim": 30294,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/org/apache/xerces/internal/dom/events",
		                   "name": "events ∑117 (117)",
		                   "data": {
		                        "$area": 117,
		                        "$dim": 117,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/java/swing",
		                   "name": "sun.java.swing ∑108100 (252)",
		                   "data": {
		                        "$area": 108100,
		                        "$dim": 108100,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/java/swing/plaf/motif",
		                   "name": "plaf.motif ∑25902 (22866)",
		                   "data": {
		                        "$area": 25902,
		                        "$dim": 25902,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/java/swing/plaf/motif/resources",
		                   "name": "resources ∑3036 (3036)",
		                   "data": {
		                        "$area": 3036,
		                        "$dim": 3036,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/java/swing/plaf/nimbus",
		                   "name": "plaf.nimbus ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/java/swing/plaf/windows",
		                   "name": "plaf.windows ∑37905 (33621)",
		                   "data": {
		                        "$area": 37905,
		                        "$dim": 37905,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/java/swing/plaf/windows/resources",
		                   "name": "resources ∑4284 (4284)",
		                   "data": {
		                        "$area": 4284,
		                        "$dim": 4284,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/java/swing/plaf/gtk",
		                   "name": "plaf.gtk ∑44035 (39283)",
		                   "data": {
		                        "$area": 44035,
		                        "$dim": 44035,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/java/swing/plaf/gtk/resources",
		                   "name": "resources ∑4752 (4752)",
		                   "data": {
		                        "$area": 4752,
		                        "$dim": 4752,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/jndi/url/corbaname",
		                   "name": "sun.jndi.url.corbaname ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/url/ldap",
		                   "name": "sun.jndi.url.ldap ∑1010 (1007)",
		                   "data": {
		                        "$area": 1010,
		                        "$dim": 1010,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/jndi/url/ldaps",
		                   "name": " ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/jndi/rmi/registry",
		                   "name": "sun.jndi.rmi.registry ∑1087 (1087)",
		                   "data": {
		                        "$area": 1087,
		                        "$dim": 1087,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/glassfish/external/amx",
		                   "name": "sun.org.glassfish.external.amx ∑711 (711)",
		                   "data": {
		                        "$area": 711,
		                        "$dim": 711,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/org/glassfish/external/statistics/impl",
		                   "name": "sun.org.glassfish.external.statistics.impl ∑1618 (1618)",
		                   "data": {
		                        "$area": 1618,
		                        "$dim": 1618,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/runtime/config",
		                   "name": "sun.xml.internal.ws.runtime.config ∑476 (476)",
		                   "data": {
		                        "$area": 476,
		                        "$dim": 476,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/transport",
		                   "name": "sun.xml.internal.ws.transport ∑7369 (280)",
		                   "data": {
		                        "$area": 7369,
		                        "$dim": 7369,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/transport/http",
		                   "name": "http ∑7089 (3219)",
		                   "data": {
		                        "$area": 7089,
		                        "$dim": 7089,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/transport/http/server",
		                   "name": "server ∑2438 (2438)",
		                   "data": {
		                        "$area": 2438,
		                        "$dim": 2438,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/transport/http/client",
		                   "name": "client ∑1432 (1432)",
		                   "data": {
		                        "$area": 1432,
		                        "$dim": 1432,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "com/sun/corba/se/impl/naming/pcosnaming",
		                   "name": "sun.corba.se.impl.naming.pcosnaming ∑2224 (2224)",
		                   "data": {
		                        "$area": 2224,
		                        "$dim": 2224,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/protocol",
		                   "name": "sun.corba.se.impl.protocol ∑14425 (8997)",
		                   "data": {
		                        "$area": 14425,
		                        "$dim": 14425,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/corba/se/impl/protocol/giopmsgheaders",
		                   "name": "giopmsgheaders ∑5428 (5428)",
		                   "data": {
		                        "$area": 5428,
		                        "$dim": 5428,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/impl/monitoring",
		                   "name": "sun.corba.se.impl.monitoring ∑336 (336)",
		                   "data": {
		                        "$area": 336,
		                        "$dim": 336,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/net/ssl",
		                   "name": "sun.net.ssl ∑1844 (1348)",
		                   "data": {
		                        "$area": 1844,
		                        "$dim": 1844,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/net/ssl/internal/www/protocol/https",
		                   "name": "internal.www.protocol.https ∑496 (496)",
		                   "data": {
		                        "$area": 496,
		                        "$dim": 496,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/org/apache/xerces/internal/parsers",
		                   "name": "sun.org.apache.xerces.internal.parsers ∑18281 (18281)",
		                   "data": {
		                        "$area": 18281,
		                        "$dim": 18281,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/security/jgss",
		                   "name": "sun.security.jgss ∑113 (113)",
		                   "data": {
		                        "$area": 113,
		                        "$dim": 113,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/spi/oa",
		                   "name": "sun.corba.se.spi.oa ∑240 (240)",
		                   "data": {
		                        "$area": 240,
		                        "$dim": 240,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/impl/corba",
		                   "name": "sun.corba.se.impl.corba ∑7589 (7589)",
		                   "data": {
		                        "$area": 7589,
		                        "$dim": 7589,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/org/objectweb/asm",
		                   "name": "sun.xml.internal.ws.org.objectweb.asm ∑16180 (16180)",
		                   "data": {
		                        "$area": 16180,
		                        "$dim": 16180,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/corba/se/internal/iiop",
		                   "name": "sun.corba.se.internal.iiop ∑3 (3)",
		                   "data": {
		                        "$area": 3,
		                        "$dim": 3,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/policy",
		                   "name": "sun.xml.internal.ws.policy ∑18669 (4995)",
		                   "data": {
		                        "$area": 18669,
		                        "$dim": 18669,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/policy/sourcemodel",
		                   "name": "sourcemodel ∑5522 (4603)",
		                   "data": {
		                        "$area": 5522,
		                        "$dim": 5522,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/ws/policy/sourcemodel/wspolicy",
		                   "name": "wspolicy ∑455 (455)",
		                   "data": {
		                        "$area": 455,
		                        "$dim": 455,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/policy/sourcemodel/attach",
		                   "name": "attach ∑464 (464)",
		                   "data": {
		                        "$area": 464,
		                        "$dim": 464,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/xml/internal/ws/policy/subject",
		                   "name": "subject ∑490 (490)",
		                   "data": {
		                        "$area": 490,
		                        "$dim": 490,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/policy/jaxws",
		                   "name": "jaxws ∑4745 (4745)",
		                   "data": {
		                        "$area": 4745,
		                        "$dim": 4745,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/policy/privateutil",
		                   "name": "privateutil ∑2710 (2710)",
		                   "data": {
		                        "$area": 2710,
		                        "$dim": 2710,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/ws/policy/spi",
		                   "name": "spi ∑207 (207)",
		                   "data": {
		                        "$area": 207,
		                        "$dim": 207,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/corba/se/impl/util",
		                   "name": "sun.corba.se.impl.util ∑5159 (5159)",
		                   "data": {
		                        "$area": 5159,
		                        "$dim": 5159,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jndi/cosnaming",
		                   "name": "sun.jndi.cosnaming ∑3330 (3330)",
		                   "data": {
		                        "$area": 3330,
		                        "$dim": 3330,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/jmx/remote/internal",
		                   "name": "sun.jmx.remote.internal ∑5301 (5301)",
		                   "data": {
		                        "$area": 5301,
		                        "$dim": 5301,
		                        "$color": "#80c080"
		                    }},{
		"id": "com/sun/xml/internal/org/jvnet/fastinfoset",
		                   "name": "sun.xml.internal.org.jvnet.fastinfoset ∑1332 (185)",
		                   "data": {
		                        "$area": 1332,
		                        "$dim": 1332,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "com/sun/xml/internal/org/jvnet/fastinfoset/sax/helpers",
		                   "name": "sax.helpers ∑1147 (1147)",
		                   "data": {
		                        "$area": 1147,
		                        "$dim": 1147,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "com/sun/imageio/plugins/wbmp",
		                   "name": "sun.imageio.plugins.wbmp ∑1670 (1670)",
		                   "data": {
		                        "$area": 1670,
		                        "$dim": 1670,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org",
		                   "name": "org ∑62318 (0)",
		                   "data": {
		                        "$area": 62318,
		                        "$dim": 62318,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "org/w3c/dom",
		                   "name": "w3c.dom ∑393 (7)",
		                   "data": {
		                        "$area": 393,
		                        "$dim": 393,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/w3c/dom/bootstrap",
		                   "name": "bootstrap ∑358 (358)",
		                   "data": {
		                        "$area": 358,
		                        "$dim": 358,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/w3c/dom/ranges",
		                   "name": "ranges ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/w3c/dom/xpath",
		                   "name": "xpath ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/w3c/dom/events",
		                   "name": "events ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/w3c/dom/ls",
		                   "name": "ls ∑7 (7)",
		                   "data": {
		                        "$area": 7,
		                        "$dim": 7,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/Dynamic",
		                   "name": "omg.Dynamic ∑12788 (24)",
		                   "data": {
		                        "$area": 12788,
		                        "$dim": 12788,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/DynamicAny",
		                   "name": "ny ∑12764 (12458)",
		                   "data": {
		                        "$area": 12764,
		                        "$dim": 12764,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/DynamicAny/DynAnyFactoryPackage",
		                   "name": "DynAnyFactoryPackage ∑102 (102)",
		                   "data": {
		                        "$area": 102,
		                        "$dim": 102,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/DynamicAny/DynAnyPackage",
		                   "name": "DynAnyPackage ∑204 (204)",
		                   "data": {
		                        "$area": 204,
		                        "$dim": 204,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "org/omg/CosNaming",
		                   "name": "omg.CosNaming ∑6699 (5461)",
		                   "data": {
		                        "$area": 6699,
		                        "$dim": 6699,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/CosNaming/NamingContextPackage",
		                   "name": "NamingContextPackage ∑947 (947)",
		                   "data": {
		                        "$area": 947,
		                        "$dim": 947,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/CosNaming/NamingContextExtPackage",
		                   "name": "NamingContextExtPackage ∑291 (291)",
		                   "data": {
		                        "$area": 291,
		                        "$dim": 291,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/CORBA",
		                   "name": "omg.CORBA ∑8024 (7380)",
		                   "data": {
		                        "$area": 8024,
		                        "$dim": 8024,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/CORBA/DynAnyPackage",
		                   "name": "DynAnyPackage ∑28 (28)",
		                   "data": {
		                        "$area": 28,
		                        "$dim": 28,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/CORBA/portable",
		                   "name": "portable ∑394 (394)",
		                   "data": {
		                        "$area": 394,
		                        "$dim": 394,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/CORBA/TypeCodePackage",
		                   "name": "TypeCodePackage ∑14 (14)",
		                   "data": {
		                        "$area": 14,
		                        "$dim": 14,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/CORBA_2_3",
		                   "name": "2_3 ∑194 (23)",
		                   "data": {
		                        "$area": 194,
		                        "$dim": 194,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/CORBA_2_3/portable",
		                   "name": "portable ∑171 (171)",
		                   "data": {
		                        "$area": 171,
		                        "$dim": 171,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/CORBA/ORBPackage",
		                   "name": "ORBPackage ∑14 (14)",
		                   "data": {
		                        "$area": 14,
		                        "$dim": 14,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/stub/javax/management/remote/rmi",
		                   "name": "omg.stub.javax.management.remote.rmi ∑10326 (10326)",
		                   "data": {
		                        "$area": 10326,
		                        "$dim": 10326,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/PortableServer",
		                   "name": "omg.PortableServer ∑2618 (1280)",
		                   "data": {
		                        "$area": 2618,
		                        "$dim": 2618,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/PortableServer/ServantLocatorPackage",
		                   "name": "ServantLocatorPackage ∑21 (21)",
		                   "data": {
		                        "$area": 21,
		                        "$dim": 21,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/PortableServer/POAPackage",
		                   "name": "POAPackage ∑1060 (1060)",
		                   "data": {
		                        "$area": 1060,
		                        "$dim": 1060,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/PortableServer/POAManagerPackage",
		                   "name": "POAManagerPackage ∑155 (155)",
		                   "data": {
		                        "$area": 155,
		                        "$dim": 155,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/PortableServer/CurrentPackage",
		                   "name": "CurrentPackage ∑102 (102)",
		                   "data": {
		                        "$area": 102,
		                        "$dim": 102,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/stub/java/rmi",
		                   "name": "omg.stub.java.rmi ∑16 (16)",
		                   "data": {
		                        "$area": 16,
		                        "$dim": 16,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/IOP",
		                   "name": "omg.IOP ∑1717 (1309)",
		                   "data": {
		                        "$area": 1717,
		                        "$dim": 1717,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/IOP/CodecPackage",
		                   "name": "CodecPackage ∑306 (306)",
		                   "data": {
		                        "$area": 306,
		                        "$dim": 306,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/IOP/CodecFactoryPackage",
		                   "name": "CodecFactoryPackage ∑102 (102)",
		                   "data": {
		                        "$area": 102,
		                        "$dim": 102,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/jcp/xml/dsig/internal",
		                   "name": "jcp.xml.dsig.internal ∑13343 (179)",
		                   "data": {
		                        "$area": 13343,
		                        "$dim": 13343,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/jcp/xml/dsig/internal/dom",
		                   "name": "dom ∑13164 (13164)",
		                   "data": {
		                        "$area": 13164,
		                        "$dim": 13164,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/omg/Messaging",
		                   "name": "omg.Messaging ∑54 (54)",
		                   "data": {
		                        "$area": 54,
		                        "$dim": 54,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/ietf/jgss",
		                   "name": "ietf.jgss ∑670 (670)",
		                   "data": {
		                        "$area": 670,
		                        "$dim": 670,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/omg/PortableInterceptor",
		                   "name": "omg.PortableInterceptor ∑1401 (1103)",
		                   "data": {
		                        "$area": 1401,
		                        "$dim": 1401,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/omg/PortableInterceptor/ORBInitInfoPackage",
		                   "name": "ORBInitInfoPackage ∑298 (298)",
		                   "data": {
		                        "$area": 298,
		                        "$dim": 298,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "org/xml/sax",
		                   "name": "xml.sax ∑4269 (311)",
		                   "data": {
		                        "$area": 4269,
		                        "$dim": 4269,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "org/xml/sax/ext",
		                   "name": "ext ∑451 (451)",
		                   "data": {
		                        "$area": 451,
		                        "$dim": 451,
		                        "$color": "#80c080"
		                    }},{
		"id": "org/xml/sax/helpers",
		                   "name": "helpers ∑3507 (3507)",
		                   "data": {
		                        "$area": 3507,
		                        "$dim": 3507,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "jdk",
		                   "name": "jdk ∑62228 (0)",
		                   "data": {
		                        "$area": 62228,
		                        "$dim": 62228,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "jdk/internal/org/xml/sax",
		                   "name": "internal.org.xml.sax ∑314 (292)",
		                   "data": {
		                        "$area": 314,
		                        "$dim": 314,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "jdk/internal/org/xml/sax/helpers",
		                   "name": "helpers ∑22 (22)",
		                   "data": {
		                        "$area": 22,
		                        "$dim": 22,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "jdk/internal/util/xml",
		                   "name": "internal.util.xml ∑7984 (496)",
		                   "data": {
		                        "$area": 7984,
		                        "$dim": 7984,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "jdk/internal/util/xml/impl",
		                   "name": "impl ∑7488 (7488)",
		                   "data": {
		                        "$area": 7488,
		                        "$dim": 7488,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "jdk/internal/org/objectweb/asm",
		                   "name": "internal.org.objectweb.asm ∑53930 (20706)",
		                   "data": {
		                        "$area": 53930,
		                        "$dim": 53930,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "jdk/internal/org/objectweb/asm/signature",
		                   "name": "signature ∑544 (544)",
		                   "data": {
		                        "$area": 544,
		                        "$dim": 544,
		                        "$color": "#80c080"
		                    }},{
		"id": "jdk/internal/org/objectweb/asm/commons",
		                   "name": "commons ∑9666 (9666)",
		                   "data": {
		                        "$area": 9666,
		                        "$dim": 9666,
		                        "$color": "#80c080"
		                    }},{
		"id": "jdk/internal/org/objectweb/asm/tree",
		                   "name": "tree ∑9279 (4706)",
		                   "data": {
		                        "$area": 9279,
		                        "$dim": 9279,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "jdk/internal/org/objectweb/asm/tree/analysis",
		                   "name": "analysis ∑4573 (4573)",
		                   "data": {
		                        "$area": 4573,
		                        "$dim": 4573,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "jdk/internal/org/objectweb/asm/util",
		                   "name": "util ∑13735 (13735)",
		                   "data": {
		                        "$area": 13735,
		                        "$dim": 13735,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "java",
		                   "name": "java ∑1520194 (0)",
		                   "data": {
		                        "$area": 1520194,
		                        "$dim": 1520194,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "javax/naming",
		                   "name": ".naming ∑10169 (3800)",
		                   "data": {
		                        "$area": 10169,
		                        "$dim": 10169,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/naming/event",
		                   "name": "event ∑73 (73)",
		                   "data": {
		                        "$area": 73,
		                        "$dim": 73,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/spi",
		                   "name": "spi ∑1724 (1724)",
		                   "data": {
		                        "$area": 1724,
		                        "$dim": 1724,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/directory",
		                   "name": "directory ∑1493 (1493)",
		                   "data": {
		                        "$area": 1493,
		                        "$dim": 1493,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/naming/ldap",
		                   "name": "ldap ∑3079 (3079)",
		                   "data": {
		                        "$area": 3079,
		                        "$dim": 3079,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/smartcardio",
		                   "name": ".smartcardio ∑1819 (1819)",
		                   "data": {
		                        "$area": 1819,
		                        "$dim": 1819,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/math",
		                   "name": "math ∑25621 (25621)",
		                   "data": {
		                        "$area": 25621,
		                        "$dim": 25621,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/net",
		                   "name": ".net ∑2349 (126)",
		                   "data": {
		                        "$area": 2349,
		                        "$dim": 2349,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/net/ssl",
		                   "name": "ssl ∑2223 (2223)",
		                   "data": {
		                        "$area": 2223,
		                        "$dim": 2223,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/sound/sampled",
		                   "name": ".sound.sampled ∑4205 (4041)",
		                   "data": {
		                        "$area": 4205,
		                        "$dim": 4205,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sound/sampled/spi",
		                   "name": "spi ∑164 (164)",
		                   "data": {
		                        "$area": 164,
		                        "$dim": 164,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/beans",
		                   "name": "beans ∑20276 (16356)",
		                   "data": {
		                        "$area": 20276,
		                        "$dim": 20276,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/beans/beancontext",
		                   "name": "beancontext ∑3920 (3920)",
		                   "data": {
		                        "$area": 3920,
		                        "$dim": 3920,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/sql",
		                   "name": "sql ∑4291 (4291)",
		                   "data": {
		                        "$area": 4291,
		                        "$dim": 4291,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt",
		                   "name": "awt ∑170687 (79040)",
		                   "data": {
		                        "$area": 170687,
		                        "$dim": 170687,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/awt/dnd",
		                   "name": "dnd ∑3092 (3092)",
		                   "data": {
		                        "$area": 3092,
		                        "$dim": 3092,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/event",
		                   "name": "event ∑4030 (4030)",
		                   "data": {
		                        "$area": 4030,
		                        "$dim": 4030,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/print",
		                   "name": "print ∑820 (820)",
		                   "data": {
		                        "$area": 820,
		                        "$dim": 820,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/color",
		                   "name": "color ∑2884 (2884)",
		                   "data": {
		                        "$area": 2884,
		                        "$dim": 2884,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/datatransfer",
		                   "name": "datatransfer ∑4643 (4643)",
		                   "data": {
		                        "$area": 4643,
		                        "$dim": 4643,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/geom",
		                   "name": "geom ∑23512 (23512)",
		                   "data": {
		                        "$area": 23512,
		                        "$dim": 23512,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/awt/im",
		                   "name": "im ∑37291 (148)",
		                   "data": {
		                        "$area": 37291,
		                        "$dim": 37291,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/awt/image",
		                   "name": "ge ∑37143 (36188)",
		                   "data": {
		                        "$area": 37143,
		                        "$dim": 37143,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/awt/image/renderable",
		                   "name": "renderable ∑955 (955)",
		                   "data": {
		                        "$area": 955,
		                        "$dim": 955,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "java/awt/font",
		                   "name": "font ∑15375 (15375)",
		                   "data": {
		                        "$area": 15375,
		                        "$dim": 15375,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/security",
		                   "name": "security ∑26457 (18118)",
		                   "data": {
		                        "$area": 26457,
		                        "$dim": 26457,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/security/acl",
		                   "name": "acl ∑9 (9)",
		                   "data": {
		                        "$area": 9,
		                        "$dim": 9,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/security/spec",
		                   "name": "spec ∑1391 (1391)",
		                   "data": {
		                        "$area": 1391,
		                        "$dim": 1391,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/security/cert",
		                   "name": "cert ∑6939 (6939)",
		                   "data": {
		                        "$area": 6939,
		                        "$dim": 6939,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/script",
		                   "name": ".script ∑1191 (1191)",
		                   "data": {
		                        "$area": 1191,
		                        "$dim": 1191,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio",
		                   "name": ".imageio ∑20867 (6925)",
		                   "data": {
		                        "$area": 20867,
		                        "$dim": 20867,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/imageio/plugins/bmp",
		                   "name": "plugins.bmp ∑33 (33)",
		                   "data": {
		                        "$area": 33,
		                        "$dim": 33,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/plugins/jpeg",
		                   "name": "plugins.jpeg ∑3502 (3502)",
		                   "data": {
		                        "$area": 3502,
		                        "$dim": 3502,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/metadata",
		                   "name": "metadata ∑2782 (2782)",
		                   "data": {
		                        "$area": 2782,
		                        "$dim": 2782,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/stream",
		                   "name": "stream ∑5608 (5608)",
		                   "data": {
		                        "$area": 5608,
		                        "$dim": 5608,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/imageio/spi",
		                   "name": "spi ∑2017 (2017)",
		                   "data": {
		                        "$area": 2017,
		                        "$dim": 2017,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/security/auth",
		                   "name": ".security.auth ∑9001 (3438)",
		                   "data": {
		                        "$area": 9001,
		                        "$dim": 9001,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/security/auth/callback",
		                   "name": "callback ∑588 (588)",
		                   "data": {
		                        "$area": 588,
		                        "$dim": 588,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/kerberos",
		                   "name": "kerberos ∑2995 (2995)",
		                   "data": {
		                        "$area": 2995,
		                        "$dim": 2995,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/login",
		                   "name": "login ∑1643 (1643)",
		                   "data": {
		                        "$area": 1643,
		                        "$dim": 1643,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/security/auth/x500",
		                   "name": "x500 ∑337 (337)",
		                   "data": {
		                        "$area": 337,
		                        "$dim": 337,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/annotation",
		                   "name": ".annotation ∑263 (38)",
		                   "data": {
		                        "$area": 263,
		                        "$dim": 263,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/annotation/processing",
		                   "name": "processing ∑225 (225)",
		                   "data": {
		                        "$area": 225,
		                        "$dim": 225,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/io",
		                   "name": "io ∑30707 (30707)",
		                   "data": {
		                        "$area": 30707,
		                        "$dim": 30707,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/tools",
		                   "name": ".tools ∑1033 (1033)",
		                   "data": {
		                        "$area": 1033,
		                        "$dim": 1033,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing",
		                   "name": ".swing ∑576204 (116338)",
		                   "data": {
		                        "$area": 576204,
		                        "$dim": 576204,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/border",
		                   "name": "border ∑2945 (2945)",
		                   "data": {
		                        "$area": 2945,
		                        "$dim": 2945,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/undo",
		                   "name": "undo ∑1102 (1102)",
		                   "data": {
		                        "$area": 1102,
		                        "$dim": 1102,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/tree",
		                   "name": "tree ∑10912 (10912)",
		                   "data": {
		                        "$area": 10912,
		                        "$dim": 10912,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/filechooser",
		                   "name": "filechooser ∑1175 (1175)",
		                   "data": {
		                        "$area": 1175,
		                        "$dim": 1175,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/colorchooser",
		                   "name": "colorchooser ∑8989 (8989)",
		                   "data": {
		                        "$area": 8989,
		                        "$dim": 8989,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/table",
		                   "name": "table ∑3740 (3740)",
		                   "data": {
		                        "$area": 3740,
		                        "$dim": 3740,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/text",
		                   "name": "text ∑110764 (52168)",
		                   "data": {
		                        "$area": 110764,
		                        "$dim": 110764,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/text/html",
		                   "name": "html ∑52370 (45152)",
		                   "data": {
		                        "$area": 52370,
		                        "$dim": 52370,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/text/html/parser",
		                   "name": "parser ∑7218 (7218)",
		                   "data": {
		                        "$area": 7218,
		                        "$dim": 7218,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/text/rtf",
		                   "name": "rtf ∑6226 (6226)",
		                   "data": {
		                        "$area": 6226,
		                        "$dim": 6226,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/plaf",
		                   "name": "plaf ∑318870 (724)",
		                   "data": {
		                        "$area": 318870,
		                        "$dim": 318870,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/swing/plaf/basic",
		                   "name": "basic ∑99826 (99826)",
		                   "data": {
		                        "$area": 99826,
		                        "$dim": 99826,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/multi",
		                   "name": "multi ∑10326 (10326)",
		                   "data": {
		                        "$area": 10326,
		                        "$dim": 10326,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/synth",
		                   "name": "synth ∑41219 (41219)",
		                   "data": {
		                        "$area": 41219,
		                        "$dim": 41219,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/nimbus",
		                   "name": "nimbus ∑124742 (124742)",
		                   "data": {
		                        "$area": 124742,
		                        "$dim": 124742,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/swing/plaf/metal",
		                   "name": "metal ∑42033 (42033)",
		                   "data": {
		                        "$area": 42033,
		                        "$dim": 42033,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/swing/event",
		                   "name": "event ∑1369 (1369)",
		                   "data": {
		                        "$area": 1369,
		                        "$dim": 1369,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/lang",
		                   "name": "lang ∑83710 (47142)",
		                   "data": {
		                        "$area": 83710,
		                        "$dim": 83710,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/lang/ref",
		                   "name": "ref ∑5120 (769)",
		                   "data": {
		                        "$area": 5120,
		                        "$dim": 5120,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/lang/reflect",
		                   "name": "ect ∑4351 (4351)",
		                   "data": {
		                        "$area": 4351,
		                        "$dim": 4351,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/lang/instrument",
		                   "name": "instrument ∑37 (37)",
		                   "data": {
		                        "$area": 37,
		                        "$dim": 37,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/lang/management",
		                   "name": "management ∑2602 (2602)",
		                   "data": {
		                        "$area": 2602,
		                        "$dim": 2602,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/lang/invoke",
		                   "name": "invoke ∑28574 (28574)",
		                   "data": {
		                        "$area": 28574,
		                        "$dim": 28574,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/lang/annotation",
		                   "name": "annotation ∑235 (235)",
		                   "data": {
		                        "$area": 235,
		                        "$dim": 235,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/jws",
		                   "name": ".jws ∑162 (48)",
		                   "data": {
		                        "$area": 162,
		                        "$dim": 162,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/jws/soap",
		                   "name": "soap ∑114 (114)",
		                   "data": {
		                        "$area": 114,
		                        "$dim": 114,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/activation",
		                   "name": ".activation ∑3993 (3993)",
		                   "data": {
		                        "$area": 3993,
		                        "$dim": 3993,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/net",
		                   "name": "net ∑32001 (32001)",
		                   "data": {
		                        "$area": 32001,
		                        "$dim": 32001,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util",
		                   "name": "util ∑248480 (111833)",
		                   "data": {
		                        "$area": 248480,
		                        "$dim": 248480,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/util/zip",
		                   "name": "zip ∑7610 (7610)",
		                   "data": {
		                        "$area": 7610,
		                        "$dim": 7610,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/logging",
		                   "name": "logging ∑8048 (8048)",
		                   "data": {
		                        "$area": 8048,
		                        "$dim": 8048,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/prefs",
		                   "name": "prefs ∑7615 (7615)",
		                   "data": {
		                        "$area": 7615,
		                        "$dim": 7615,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/stream",
		                   "name": "stream ∑23928 (23928)",
		                   "data": {
		                        "$area": 23928,
		                        "$dim": 23928,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/regex",
		                   "name": "regex ∑12944 (12944)",
		                   "data": {
		                        "$area": 12944,
		                        "$dim": 12944,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/function",
		                   "name": "function ∑502 (502)",
		                   "data": {
		                        "$area": 502,
		                        "$dim": 502,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/concurrent",
		                   "name": "concurrent ∑71038 (57749)",
		                   "data": {
		                        "$area": 71038,
		                        "$dim": 71038,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/util/concurrent/atomic",
		                   "name": "atomic ∑5182 (5182)",
		                   "data": {
		                        "$area": 5182,
		                        "$dim": 5182,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/concurrent/locks",
		                   "name": "locks ∑8107 (8107)",
		                   "data": {
		                        "$area": 8107,
		                        "$dim": 8107,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/util/spi",
		                   "name": "spi ∑132 (132)",
		                   "data": {
		                        "$area": 132,
		                        "$dim": 132,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/util/jar",
		                   "name": "jar ∑4830 (4830)",
		                   "data": {
		                        "$area": 4830,
		                        "$dim": 4830,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/print",
		                   "name": ".print ∑11404 (2798)",
		                   "data": {
		                        "$area": 11404,
		                        "$dim": 11404,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/print/event",
		                   "name": "event ∑68 (68)",
		                   "data": {
		                        "$area": 68,
		                        "$dim": 68,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/print/attribute",
		                   "name": "attribute ∑8538 (2326)",
		                   "data": {
		                        "$area": 8538,
		                        "$dim": 8538,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/print/attribute/standard",
		                   "name": "standard ∑6212 (6212)",
		                   "data": {
		                        "$area": 6212,
		                        "$dim": 6212,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "javax/sql",
		                   "name": ".sql ∑7084 (50)",
		                   "data": {
		                        "$area": 7084,
		                        "$dim": 7084,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sql/rowset",
		                   "name": "rowset ∑7034 (2577)",
		                   "data": {
		                        "$area": 7034,
		                        "$dim": 7034,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sql/rowset/serial",
		                   "name": "serial ∑3490 (3490)",
		                   "data": {
		                        "$area": 3490,
		                        "$dim": 3490,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/sql/rowset/spi",
		                   "name": "spi ∑967 (967)",
		                   "data": {
		                        "$area": 967,
		                        "$dim": 967,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "java/rmi",
		                   "name": "rmi ∑3916 (824)",
		                   "data": {
		                        "$area": 3916,
		                        "$dim": 3916,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/rmi/dgc",
		                   "name": "dgc ∑165 (165)",
		                   "data": {
		                        "$area": 165,
		                        "$dim": 165,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/rmi/registry",
		                   "name": "registry ∑84 (84)",
		                   "data": {
		                        "$area": 84,
		                        "$dim": 84,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/rmi/server",
		                   "name": "server ∑1601 (1601)",
		                   "data": {
		                        "$area": 1601,
		                        "$dim": 1601,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/rmi/activation",
		                   "name": "activation ∑1242 (1242)",
		                   "data": {
		                        "$area": 1242,
		                        "$dim": 1242,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml",
		                   "name": ".xml ∑19796 (3)",
		                   "data": {
		                        "$area": 19796,
		                        "$dim": 19796,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/parsers",
		                   "name": "parsers ∑1337 (1337)",
		                   "data": {
		                        "$area": 1337,
		                        "$dim": 1337,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/namespace",
		                   "name": "namespace ∑240 (240)",
		                   "data": {
		                        "$area": 240,
		                        "$dim": 240,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws",
		                   "name": "ws ∑1325 (347)",
		                   "data": {
		                        "$area": 1325,
		                        "$dim": 1325,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/ws/spi",
		                   "name": "spi ∑507 (494)",
		                   "data": {
		                        "$area": 507,
		                        "$dim": 507,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/ws/spi/http",
		                   "name": "http ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/ws/wsaddressing",
		                   "name": "wsaddressing ∑258 (258)",
		                   "data": {
		                        "$area": 258,
		                        "$dim": 258,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/http",
		                   "name": "http ∑9 (9)",
		                   "data": {
		                        "$area": 9,
		                        "$dim": 9,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/soap",
		                   "name": "soap ∑166 (166)",
		                   "data": {
		                        "$area": 166,
		                        "$dim": 166,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/ws/handler",
		                   "name": "handler ∑38 (38)",
		                   "data": {
		                        "$area": 38,
		                        "$dim": 38,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/datatype",
		                   "name": "datatype ∑1615 (1615)",
		                   "data": {
		                        "$area": 1615,
		                        "$dim": 1615,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/xpath",
		                   "name": "xpath ∑1242 (1242)",
		                   "data": {
		                        "$area": 1242,
		                        "$dim": 1242,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/validation",
		                   "name": "validation ∑1260 (1260)",
		                   "data": {
		                        "$area": 1260,
		                        "$dim": 1260,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/stream",
		                   "name": "stream ∑1145 (876)",
		                   "data": {
		                        "$area": 1145,
		                        "$dim": 1145,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/stream/util",
		                   "name": "util ∑269 (269)",
		                   "data": {
		                        "$area": 269,
		                        "$dim": 269,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/soap",
		                   "name": "soap ∑1038 (1038)",
		                   "data": {
		                        "$area": 1038,
		                        "$dim": 1038,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto",
		                   "name": "crypto ∑1744 (287)",
		                   "data": {
		                        "$area": 1744,
		                        "$dim": 1744,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/crypto/dsig",
		                   "name": "dsig ∑1244 (601)",
		                   "data": {
		                        "$area": 1244,
		                        "$dim": 1244,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/crypto/dsig/keyinfo",
		                   "name": "keyinfo ∑131 (131)",
		                   "data": {
		                        "$area": 131,
		                        "$dim": 131,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto/dsig/dom",
		                   "name": "dom ∑198 (198)",
		                   "data": {
		                        "$area": 198,
		                        "$dim": 198,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/crypto/dsig/spec",
		                   "name": "spec ∑314 (314)",
		                   "data": {
		                        "$area": 314,
		                        "$dim": 314,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/crypto/dom",
		                   "name": "dom ∑213 (213)",
		                   "data": {
		                        "$area": 213,
		                        "$dim": 213,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/transform",
		                   "name": "transform ∑1768 (1108)",
		                   "data": {
		                        "$area": 1768,
		                        "$dim": 1768,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/transform/stax",
		                   "name": "stax ∑156 (156)",
		                   "data": {
		                        "$area": 156,
		                        "$dim": 156,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/stream",
		                   "name": "stream ∑137 (137)",
		                   "data": {
		                        "$area": 137,
		                        "$dim": 137,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/sax",
		                   "name": "sax ∑125 (125)",
		                   "data": {
		                        "$area": 125,
		                        "$dim": 125,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/transform/dom",
		                   "name": "dom ∑242 (242)",
		                   "data": {
		                        "$area": 242,
		                        "$dim": 242,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/xml/bind",
		                   "name": "bind ∑7079 (4516)",
		                   "data": {
		                        "$area": 7079,
		                        "$dim": 7079,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/bind/util",
		                   "name": "util ∑370 (370)",
		                   "data": {
		                        "$area": 370,
		                        "$dim": 370,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/helpers",
		                   "name": "helpers ∑1679 (1679)",
		                   "data": {
		                        "$area": 1679,
		                        "$dim": 1679,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/attachment",
		                   "name": "attachment ∑10 (10)",
		                   "data": {
		                        "$area": 10,
		                        "$dim": 10,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/xml/bind/annotation",
		                   "name": "annotation ∑504 (249)",
		                   "data": {
		                        "$area": 504,
		                        "$dim": 504,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/xml/bind/annotation/adapters",
		                   "name": "adapters ∑255 (255)",
		                   "data": {
		                        "$area": 255,
		                        "$dim": 255,
		                        "$color": "#80c080"
		                    }}]
		}]
		}]
		},{
		"id": "javax/transaction",
		                   "name": ".transaction ∑34 (21)",
		                   "data": {
		                        "$area": 34,
		                        "$dim": 34,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/transaction/xa",
		                   "name": "xa ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/lang/model",
		                   "name": ".lang.model ∑2573 (465)",
		                   "data": {
		                        "$area": 2573,
		                        "$dim": 2573,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/lang/model/element",
		                   "name": "element ∑472 (472)",
		                   "data": {
		                        "$area": 472,
		                        "$dim": 472,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/lang/model/util",
		                   "name": "util ∑1241 (1241)",
		                   "data": {
		                        "$area": 1241,
		                        "$dim": 1241,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/lang/model/type",
		                   "name": "type ∑395 (395)",
		                   "data": {
		                        "$area": 395,
		                        "$dim": 395,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/accessibility",
		                   "name": ".accessibility ∑2398 (2398)",
		                   "data": {
		                        "$area": 2398,
		                        "$dim": 2398,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management",
		                   "name": ".management ∑76954 (14395)",
		                   "data": {
		                        "$area": 76954,
		                        "$dim": 76954,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/management/monitor",
		                   "name": "monitor ∑4368 (4368)",
		                   "data": {
		                        "$area": 4368,
		                        "$dim": 4368,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/timer",
		                   "name": "timer ∑1393 (1393)",
		                   "data": {
		                        "$area": 1393,
		                        "$dim": 1393,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/relation",
		                   "name": "relation ∑9815 (9815)",
		                   "data": {
		                        "$area": 9815,
		                        "$dim": 9815,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/openmbean",
		                   "name": "openmbean ∑6572 (6572)",
		                   "data": {
		                        "$area": 6572,
		                        "$dim": 6572,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/modelmbean",
		                   "name": "modelmbean ∑11295 (11295)",
		                   "data": {
		                        "$area": 11295,
		                        "$dim": 11295,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/loading",
		                   "name": "loading ∑2795 (2795)",
		                   "data": {
		                        "$area": 2795,
		                        "$dim": 2795,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/management/remote",
		                   "name": "remote ∑26321 (2277)",
		                   "data": {
		                        "$area": 26321,
		                        "$dim": 26321,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/management/remote/rmi",
		                   "name": "rmi ∑24044 (24044)",
		                   "data": {
		                        "$area": 24044,
		                        "$dim": 24044,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "java/nio",
		                   "name": "nio ∑35362 (24633)",
		                   "data": {
		                        "$area": 35362,
		                        "$dim": 35362,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/nio/file",
		                   "name": "file ∑6065 (3944)",
		                   "data": {
		                        "$area": 6065,
		                        "$dim": 6065,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/nio/file/spi",
		                   "name": "spi ∑290 (290)",
		                   "data": {
		                        "$area": 290,
		                        "$dim": 290,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/nio/file/attribute",
		                   "name": "attribute ∑1831 (1831)",
		                   "data": {
		                        "$area": 1831,
		                        "$dim": 1831,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/nio/charset",
		                   "name": "charset ∑2148 (2135)",
		                   "data": {
		                        "$area": 2148,
		                        "$dim": 2148,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/nio/charset/spi",
		                   "name": "spi ∑13 (13)",
		                   "data": {
		                        "$area": 13,
		                        "$dim": 13,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/nio/channels",
		                   "name": "channels ∑2516 (1483)",
		                   "data": {
		                        "$area": 2516,
		                        "$dim": 2516,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/nio/channels/spi",
		                   "name": "spi ∑1033 (1033)",
		                   "data": {
		                        "$area": 1033,
		                        "$dim": 1033,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "javax/security/sasl",
		                   "name": ".security.sasl ∑502 (502)",
		                   "data": {
		                        "$area": 502,
		                        "$dim": 502,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/time",
		                   "name": "time ∑57883 (17121)",
		                   "data": {
		                        "$area": 57883,
		                        "$dim": 57883,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/time/format",
		                   "name": "format ∑20606 (20606)",
		                   "data": {
		                        "$area": 20606,
		                        "$dim": 20606,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/time/zone",
		                   "name": "zone ∑3340 (3340)",
		                   "data": {
		                        "$area": 3340,
		                        "$dim": 3340,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/time/chrono",
		                   "name": "chrono ∑12164 (12164)",
		                   "data": {
		                        "$area": 12164,
		                        "$dim": 12164,
		                        "$color": "#80c080"
		                    }},{
		"id": "java/time/temporal",
		                   "name": "temporal ∑4652 (4652)",
		                   "data": {
		                        "$area": 4652,
		                        "$dim": 4652,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/applet",
		                   "name": "applet ∑206 (206)",
		                   "data": {
		                        "$area": 206,
		                        "$dim": 206,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/rmi",
		                   "name": ".rmi ∑1357 (281)",
		                   "data": {
		                        "$area": 1357,
		                        "$dim": 1357,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/rmi/CORBA",
		                   "name": "CORBA ∑588 (588)",
		                   "data": {
		                        "$area": 588,
		                        "$dim": 588,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/rmi/ssl",
		                   "name": "ssl ∑488 (488)",
		                   "data": {
		                        "$area": 488,
		                        "$dim": 488,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "java/text",
		                   "name": "text ∑24078 (24060)",
		                   "data": {
		                        "$area": 24078,
		                        "$dim": 24078,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "java/text/spi",
		                   "name": "spi ∑18 (18)",
		                   "data": {
		                        "$area": 18,
		                        "$dim": 18,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "javax/security/cert",
		                   "name": ".security.cert ∑260 (260)",
		                   "data": {
		                        "$area": 260,
		                        "$dim": 260,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/activity",
		                   "name": ".activity ∑51 (51)",
		                   "data": {
		                        "$area": 51,
		                        "$dim": 51,
		                        "$color": "#80c080"
		                    }},{
		"id": "javax/sound/midi",
		                   "name": ".sound.midi ∑2850 (2776)",
		                   "data": {
		                        "$area": 2850,
		                        "$dim": 2850,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "javax/sound/midi/spi",
		                   "name": "spi ∑74 (74)",
		                   "data": {
		                        "$area": 74,
		                        "$dim": 74,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "apple",
		                   "name": "apple ∑5892 (0)",
		                   "data": {
		                        "$area": 5892,
		                        "$dim": 5892,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "apple/laf",
		                   "name": "laf ∑3032 (3032)",
		                   "data": {
		                        "$area": 3032,
		                        "$dim": 3032,
		                        "$color": "#80c080"
		                    }},{
		"id": "apple/security",
		                   "name": "security ∑1806 (1806)",
		                   "data": {
		                        "$area": 1806,
		                        "$dim": 1806,
		                        "$color": "#80c080"
		                    }},{
		"id": "apple/applescript",
		                   "name": "applescript ∑538 (538)",
		                   "data": {
		                        "$area": 538,
		                        "$dim": 538,
		                        "$color": "#80c080"
		                    }},{
		"id": "apple/launcher",
		                   "name": "launcher ∑516 (516)",
		                   "data": {
		                        "$area": 516,
		                        "$dim": 516,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun",
		                   "name": "sun ∑1181063 (0)",
		                   "data": {
		                        "$area": 1181063,
		                        "$dim": 1181063,
		                        "$color": "#8080b0"
		                    },"children": [{
		"id": "sun/security/tools",
		                   "name": "security.tools ∑73022 (417)",
		                   "data": {
		                        "$area": 73022,
		                        "$dim": 73022,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/tools/policytool",
		                   "name": "policytool ∑23344 (23344)",
		                   "data": {
		                        "$area": 23344,
		                        "$dim": 23344,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/tools/keytool",
		                   "name": "keytool ∑49261 (49261)",
		                   "data": {
		                        "$area": 49261,
		                        "$dim": 49261,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/rmi/log",
		                   "name": "rmi.log ∑1404 (1404)",
		                   "data": {
		                        "$area": 1404,
		                        "$dim": 1404,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/misc",
		                   "name": "misc ∑28155 (27579)",
		                   "data": {
		                        "$area": 28155,
		                        "$dim": 28155,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/misc/resources",
		                   "name": "resources ∑576 (576)",
		                   "data": {
		                        "$area": 576,
		                        "$dim": 576,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/util",
		                   "name": "util ∑65229 (862)",
		                   "data": {
		                        "$area": 65229,
		                        "$dim": 65229,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/util/locale",
		                   "name": "locale ∑17844 (7175)",
		                   "data": {
		                        "$area": 17844,
		                        "$dim": 17844,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/util/locale/provider",
		                   "name": "provider ∑10669 (10669)",
		                   "data": {
		                        "$area": 10669,
		                        "$dim": 10669,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/util/calendar",
		                   "name": "calendar ∑8801 (8801)",
		                   "data": {
		                        "$area": 8801,
		                        "$dim": 8801,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/util/resources",
		                   "name": "resources ∑34593 (32965)",
		                   "data": {
		                        "$area": 34593,
		                        "$dim": 34593,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/util/resources/en",
		                   "name": "en ∑1628 (1628)",
		                   "data": {
		                        "$area": 1628,
		                        "$dim": 1628,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/util/spi",
		                   "name": "spi ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/util/logging",
		                   "name": "logging ∑2627 (1151)",
		                   "data": {
		                        "$area": 2627,
		                        "$dim": 2627,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/util/logging/resources",
		                   "name": "resources ∑1476 (1476)",
		                   "data": {
		                        "$area": 1476,
		                        "$dim": 1476,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/util/cldr",
		                   "name": "cldr ∑129 (129)",
		                   "data": {
		                        "$area": 129,
		                        "$dim": 129,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/util/xml",
		                   "name": "xml ∑367 (367)",
		                   "data": {
		                        "$area": 367,
		                        "$dim": 367,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/util",
		                   "name": "security.util ∑31988 (31988)",
		                   "data": {
		                        "$area": 31988,
		                        "$dim": 31988,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/nio/cs",
		                   "name": "nio.cs ∑16804 (16804)",
		                   "data": {
		                        "$area": 16804,
		                        "$dim": 16804,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/dc",
		                   "name": "dc ∑2655 (1832)",
		                   "data": {
		                        "$area": 2655,
		                        "$dim": 2655,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/dc/path",
		                   "name": "path ∑14 (14)",
		                   "data": {
		                        "$area": 14,
		                        "$dim": 14,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/dc/pr",
		                   "name": "pr ∑809 (809)",
		                   "data": {
		                        "$area": 809,
		                        "$dim": 809,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/krb5",
		                   "name": "security.krb5 ∑39414 (11566)",
		                   "data": {
		                        "$area": 39414,
		                        "$dim": 39414,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/krb5/internal",
		                   "name": "internal ∑27848 (13119)",
		                   "data": {
		                        "$area": 27848,
		                        "$dim": 27848,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/krb5/internal/rcache",
		                   "name": "rcache ∑1430 (1430)",
		                   "data": {
		                        "$area": 1430,
		                        "$dim": 1430,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/krb5/internal/crypto",
		                   "name": "crypto ∑8895 (5781)",
		                   "data": {
		                        "$area": 8895,
		                        "$dim": 8895,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/krb5/internal/crypto/dk",
		                   "name": "dk ∑3114 (3114)",
		                   "data": {
		                        "$area": 3114,
		                        "$dim": 3114,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/krb5/internal/ktab",
		                   "name": "ktab ∑1506 (1506)",
		                   "data": {
		                        "$area": 1506,
		                        "$dim": 1506,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/krb5/internal/util",
		                   "name": "util ∑313 (313)",
		                   "data": {
		                        "$area": 313,
		                        "$dim": 313,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/krb5/internal/ccache",
		                   "name": "ccache ∑2585 (2585)",
		                   "data": {
		                        "$area": 2585,
		                        "$dim": 2585,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/management",
		                   "name": "management ∑40346 (10757)",
		                   "data": {
		                        "$area": 40346,
		                        "$dim": 40346,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/management/snmp",
		                   "name": "snmp ∑17166 (617)",
		                   "data": {
		                        "$area": 17166,
		                        "$dim": 17166,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/management/snmp/util",
		                   "name": "util ∑1072 (1072)",
		                   "data": {
		                        "$area": 1072,
		                        "$dim": 1072,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/management/snmp/jvmmib",
		                   "name": "jvmmib ∑8552 (8552)",
		                   "data": {
		                        "$area": 8552,
		                        "$dim": 8552,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/management/snmp/jvminstr",
		                   "name": "jvminstr ∑6925 (6925)",
		                   "data": {
		                        "$area": 6925,
		                        "$dim": 6925,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/management/jmxremote",
		                   "name": "jmxremote ∑1669 (1669)",
		                   "data": {
		                        "$area": 1669,
		                        "$dim": 1669,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/management/jdp",
		                   "name": "jdp ∑746 (746)",
		                   "data": {
		                        "$area": 746,
		                        "$dim": 746,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/management/resources",
		                   "name": "resources ∑8340 (8340)",
		                   "data": {
		                        "$area": 8340,
		                        "$dim": 8340,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/management/counter",
		                   "name": "counter ∑1668 (239)",
		                   "data": {
		                        "$area": 1668,
		                        "$dim": 1668,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/management/counter/perf",
		                   "name": "perf ∑1429 (1429)",
		                   "data": {
		                        "$area": 1429,
		                        "$dim": 1429,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/usagetracker",
		                   "name": "usagetracker ∑1064 (1064)",
		                   "data": {
		                        "$area": 1064,
		                        "$dim": 1064,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/timestamp",
		                   "name": "security.timestamp ∑755 (755)",
		                   "data": {
		                        "$area": 755,
		                        "$dim": 755,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/jgss",
		                   "name": "security.jgss ∑23242 (6402)",
		                   "data": {
		                        "$area": 23242,
		                        "$dim": 23242,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/jgss/spnego",
		                   "name": "spnego ∑3003 (3003)",
		                   "data": {
		                        "$area": 3003,
		                        "$dim": 3003,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/jgss/krb5",
		                   "name": "krb5 ∑11133 (11133)",
		                   "data": {
		                        "$area": 11133,
		                        "$dim": 11133,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/jgss/wrapper",
		                   "name": "wrapper ∑2704 (2704)",
		                   "data": {
		                        "$area": 2704,
		                        "$dim": 2704,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/smartcardio",
		                   "name": "security.smartcardio ∑2330 (2330)",
		                   "data": {
		                        "$area": 2330,
		                        "$dim": 2330,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/rmi/server",
		                   "name": "rmi.server ∑9698 (9698)",
		                   "data": {
		                        "$area": 9698,
		                        "$dim": 9698,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/tools/jar",
		                   "name": "tools.jar ∑7309 (3805)",
		                   "data": {
		                        "$area": 7309,
		                        "$dim": 7309,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/tools/jar/resources",
		                   "name": "resources ∑3504 (3504)",
		                   "data": {
		                        "$area": 3504,
		                        "$dim": 3504,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/corba",
		                   "name": "corba ∑990 (990)",
		                   "data": {
		                        "$area": 990,
		                        "$dim": 990,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/rmi/transport",
		                   "name": "rmi.transport ∑12317 (4483)",
		                   "data": {
		                        "$area": 12317,
		                        "$dim": 12317,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/rmi/transport/tcp",
		                   "name": "tcp ∑4998 (4998)",
		                   "data": {
		                        "$area": 4998,
		                        "$dim": 4998,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/rmi/transport/proxy",
		                   "name": "proxy ∑2836 (2836)",
		                   "data": {
		                        "$area": 2836,
		                        "$dim": 2836,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/tracing",
		                   "name": "tracing ∑1228 (678)",
		                   "data": {
		                        "$area": 1228,
		                        "$dim": 1228,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/tracing/dtrace",
		                   "name": "dtrace ∑550 (550)",
		                   "data": {
		                        "$area": 550,
		                        "$dim": 550,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/lwawt",
		                   "name": "lwawt ∑26071 (9394)",
		                   "data": {
		                        "$area": 26071,
		                        "$dim": 26071,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/lwawt/macosx",
		                   "name": "macosx ∑16677 (16425)",
		                   "data": {
		                        "$area": 16677,
		                        "$dim": 16677,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/lwawt/macosx/event",
		                   "name": "event ∑252 (252)",
		                   "data": {
		                        "$area": 252,
		                        "$dim": 252,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/security/validator",
		                   "name": "security.validator ∑1977 (1977)",
		                   "data": {
		                        "$area": 1977,
		                        "$dim": 1977,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/applet",
		                   "name": "applet ∑34032 (8508)",
		                   "data": {
		                        "$area": 34032,
		                        "$dim": 34032,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/applet/resources",
		                   "name": "resources ∑25524 (25524)",
		                   "data": {
		                        "$area": 25524,
		                        "$dim": 25524,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/invoke/empty",
		                   "name": "invoke.empty ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/x509",
		                   "name": "security.x509 ∑33524 (33524)",
		                   "data": {
		                        "$area": 33524,
		                        "$dim": 33524,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/instrument",
		                   "name": "instrument ∑743 (743)",
		                   "data": {
		                        "$area": 743,
		                        "$dim": 743,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/action",
		                   "name": "security.action ∑204 (204)",
		                   "data": {
		                        "$area": 204,
		                        "$dim": 204,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/launcher",
		                   "name": "launcher ∑4560 (1524)",
		                   "data": {
		                        "$area": 4560,
		                        "$dim": 4560,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/launcher/resources",
		                   "name": "resources ∑3036 (3036)",
		                   "data": {
		                        "$area": 3036,
		                        "$dim": 3036,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/nio/fs",
		                   "name": "nio.fs ∑13968 (13968)",
		                   "data": {
		                        "$area": 13968,
		                        "$dim": 13968,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt",
		                   "name": "awt ∑254064 (161259)",
		                   "data": {
		                        "$area": 254064,
		                        "$dim": 254064,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/awt/util",
		                   "name": "util ∑1575 (1575)",
		                   "data": {
		                        "$area": 1575,
		                        "$dim": 1575,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/motif",
		                   "name": "motif ∑6500 (6500)",
		                   "data": {
		                        "$area": 6500,
		                        "$dim": 6500,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/im",
		                   "name": "im ∑37757 (4639)",
		                   "data": {
		                        "$area": 37757,
		                        "$dim": 37757,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/awt/image",
		                   "name": "ge ∑33118 (29722)",
		                   "data": {
		                        "$area": 33118,
		                        "$dim": 33118,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/awt/image/codec",
		                   "name": "codec ∑3396 (3396)",
		                   "data": {
		                        "$area": 3396,
		                        "$dim": 3396,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/awt/windows",
		                   "name": "windows ∑32 (32)",
		                   "data": {
		                        "$area": 32,
		                        "$dim": 32,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/dnd",
		                   "name": "dnd ∑2115 (2115)",
		                   "data": {
		                        "$area": 2115,
		                        "$dim": 2115,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/shell",
		                   "name": "shell ∑1026 (1026)",
		                   "data": {
		                        "$area": 1026,
		                        "$dim": 1026,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/event",
		                   "name": "event ∑6 (6)",
		                   "data": {
		                        "$area": 6,
		                        "$dim": 6,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/datatransfer",
		                   "name": "datatransfer ∑6700 (6700)",
		                   "data": {
		                        "$area": 6700,
		                        "$dim": 6700,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/geom",
		                   "name": "geom ∑9703 (9703)",
		                   "data": {
		                        "$area": 9703,
		                        "$dim": 9703,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/awt/resources",
		                   "name": "resources ∑27391 (27391)",
		                   "data": {
		                        "$area": 27391,
		                        "$dim": 27391,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/font",
		                   "name": "font ∑55369 (55369)",
		                   "data": {
		                        "$area": 55369,
		                        "$dim": 55369,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/pkcs",
		                   "name": "security.pkcs ∑11260 (6138)",
		                   "data": {
		                        "$area": 11260,
		                        "$dim": 11260,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/pkcs12",
		                   "name": "2 ∑4455 (4455)",
		                   "data": {
		                        "$area": 4455,
		                        "$dim": 4455,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/pkcs10",
		                   "name": "0 ∑667 (667)",
		                   "data": {
		                        "$area": 667,
		                        "$dim": 667,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/net",
		                   "name": "net ∑63340 (19247)",
		                   "data": {
		                        "$area": 63340,
		                        "$dim": 63340,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/net/util",
		                   "name": "util ∑687 (687)",
		                   "data": {
		                        "$area": 687,
		                        "$dim": 687,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/smtp",
		                   "name": "smtp ∑499 (499)",
		                   "data": {
		                        "$area": 499,
		                        "$dim": 499,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/dns",
		                   "name": "dns ∑376 (376)",
		                   "data": {
		                        "$area": 376,
		                        "$dim": 376,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/ftp",
		                   "name": "ftp ∑5175 (1375)",
		                   "data": {
		                        "$area": 5175,
		                        "$dim": 5175,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/net/ftp/impl",
		                   "name": "impl ∑3800 (3800)",
		                   "data": {
		                        "$area": 3800,
		                        "$dim": 3800,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/net/httpserver",
		                   "name": "httpserver ∑7557 (7557)",
		                   "data": {
		                        "$area": 7557,
		                        "$dim": 7557,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/spi",
		                   "name": "spi ∑681 (681)",
		                   "data": {
		                        "$area": 681,
		                        "$dim": 681,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/sdp",
		                   "name": "sdp ∑788 (788)",
		                   "data": {
		                        "$area": 788,
		                        "$dim": 788,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www",
		                   "name": "www ∑25793 (5586)",
		                   "data": {
		                        "$area": 25793,
		                        "$dim": 25793,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/net/www/protocol/jar",
		                   "name": "protocol.jar ∑1375 (1375)",
		                   "data": {
		                        "$area": 1375,
		                        "$dim": 1375,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/http",
		                   "name": "http ∑4349 (4349)",
		                   "data": {
		                        "$area": 4349,
		                        "$dim": 4349,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/mailto",
		                   "name": "protocol.mailto ∑263 (263)",
		                   "data": {
		                        "$area": 263,
		                        "$dim": 263,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/content/image",
		                   "name": "content.image ∑230 (230)",
		                   "data": {
		                        "$area": 230,
		                        "$dim": 230,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/content/audio",
		                   "name": "content.audio ∑40 (40)",
		                   "data": {
		                        "$area": 40,
		                        "$dim": 40,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/netdoc",
		                   "name": "protocol.netdoc ∑89 (89)",
		                   "data": {
		                        "$area": 89,
		                        "$dim": 89,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/content/text",
		                   "name": "content.text ∑30 (30)",
		                   "data": {
		                        "$area": 30,
		                        "$dim": 30,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/http",
		                   "name": "protocol.http ∑12189 (9816)",
		                   "data": {
		                        "$area": 12189,
		                        "$dim": 12189,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/net/www/protocol/http/logging",
		                   "name": "logging ∑265 (265)",
		                   "data": {
		                        "$area": 265,
		                        "$dim": 265,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/https",
		                   "name": " ∑1597 (1597)",
		                   "data": {
		                        "$area": 1597,
		                        "$dim": 1597,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/http/ntlm",
		                   "name": "ntlm ∑261 (261)",
		                   "data": {
		                        "$area": 261,
		                        "$dim": 261,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/http/spnego",
		                   "name": "spnego ∑250 (250)",
		                   "data": {
		                        "$area": 250,
		                        "$dim": 250,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/net/www/protocol/ftp",
		                   "name": "protocol.ftp ∑1132 (1132)",
		                   "data": {
		                        "$area": 1132,
		                        "$dim": 1132,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/net/www/protocol/file",
		                   "name": "protocol.file ∑510 (510)",
		                   "data": {
		                        "$area": 510,
		                        "$dim": 510,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/net/idn",
		                   "name": "idn ∑2537 (2537)",
		                   "data": {
		                        "$area": 2537,
		                        "$dim": 2537,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/audio",
		                   "name": "audio ∑781 (781)",
		                   "data": {
		                        "$area": 781,
		                        "$dim": 781,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/invoke/util",
		                   "name": "invoke.util ∑6517 (6517)",
		                   "data": {
		                        "$area": 6517,
		                        "$dim": 6517,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/rmi/runtime",
		                   "name": "rmi.runtime ∑805 (805)",
		                   "data": {
		                        "$area": 805,
		                        "$dim": 805,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/print",
		                   "name": "print ∑63049 (38173)",
		                   "data": {
		                        "$area": 63049,
		                        "$dim": 63049,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/print/resources",
		                   "name": "resources ∑24876 (24876)",
		                   "data": {
		                        "$area": 24876,
		                        "$dim": 24876,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/nio/ch",
		                   "name": "nio.ch ∑29473 (27887)",
		                   "data": {
		                        "$area": 29473,
		                        "$dim": 29473,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/nio/ch/sctp",
		                   "name": "sctp ∑1586 (1586)",
		                   "data": {
		                        "$area": 1586,
		                        "$dim": 1586,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/text",
		                   "name": "text ∑30835 (2118)",
		                   "data": {
		                        "$area": 30835,
		                        "$dim": 30835,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/text/normalizer",
		                   "name": "normalizer ∑13918 (13918)",
		                   "data": {
		                        "$area": 13918,
		                        "$dim": 13918,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/text/bidi",
		                   "name": "bidi ∑8752 (8752)",
		                   "data": {
		                        "$area": 8752,
		                        "$dim": 8752,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/text/resources",
		                   "name": "resources ∑6047 (3984)",
		                   "data": {
		                        "$area": 6047,
		                        "$dim": 6047,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/text/resources/en",
		                   "name": "en ∑2063 (2063)",
		                   "data": {
		                        "$area": 2063,
		                        "$dim": 2063,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/security/acl",
		                   "name": "security.acl ∑1005 (1005)",
		                   "data": {
		                        "$area": 1005,
		                        "$dim": 1005,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect",
		                   "name": "reflect ∑20051 (11425)",
		                   "data": {
		                        "$area": 20051,
		                        "$dim": 20051,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/reflect/misc",
		                   "name": "misc ∑1035 (1035)",
		                   "data": {
		                        "$area": 1035,
		                        "$dim": 1035,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/tree",
		                   "name": "generics.tree ∑364 (364)",
		                   "data": {
		                        "$area": 364,
		                        "$dim": 364,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/repository",
		                   "name": "generics.repository ∑320 (320)",
		                   "data": {
		                        "$area": 320,
		                        "$dim": 320,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/annotation",
		                   "name": "annotation ∑4532 (4532)",
		                   "data": {
		                        "$area": 4532,
		                        "$dim": 4532,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/parser",
		                   "name": "generics.parser ∑904 (904)",
		                   "data": {
		                        "$area": 904,
		                        "$dim": 904,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/scope",
		                   "name": "generics.scope ∑138 (138)",
		                   "data": {
		                        "$area": 138,
		                        "$dim": 138,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/reflectiveObjects",
		                   "name": "generics.reflectiveObjects ∑878 (878)",
		                   "data": {
		                        "$area": 878,
		                        "$dim": 878,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/factory",
		                   "name": "generics.factory ∑131 (131)",
		                   "data": {
		                        "$area": 131,
		                        "$dim": 131,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/reflect/generics/visitor",
		                   "name": "generics.visitor ∑324 (324)",
		                   "data": {
		                        "$area": 324,
		                        "$dim": 324,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/jca",
		                   "name": "security.jca ∑1890 (1890)",
		                   "data": {
		                        "$area": 1890,
		                        "$dim": 1890,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/invoke/anon",
		                   "name": "invoke.anon ∑2206 (2206)",
		                   "data": {
		                        "$area": 2206,
		                        "$dim": 2206,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/rmi/registry",
		                   "name": "rmi.registry ∑1339 (1339)",
		                   "data": {
		                        "$area": 1339,
		                        "$dim": 1339,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/provider",
		                   "name": "security.provider ∑47687 (28120)",
		                   "data": {
		                        "$area": 47687,
		                        "$dim": 47687,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/provider/certpath",
		                   "name": "certpath ∑19567 (17809)",
		                   "data": {
		                        "$area": 19567,
		                        "$dim": 19567,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/security/provider/certpath/ldap",
		                   "name": "ldap ∑1496 (1496)",
		                   "data": {
		                        "$area": 1496,
		                        "$dim": 1496,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/security/provider/certpath/ssl",
		                   "name": "ssl ∑262 (262)",
		                   "data": {
		                        "$area": 262,
		                        "$dim": 262,
		                        "$color": "#80c080"
		                    }}]
		}]
		},{
		"id": "sun/swing",
		                   "name": "swing ∑30362 (12863)",
		                   "data": {
		                        "$area": 30362,
		                        "$dim": 30362,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/swing/plaf",
		                   "name": "plaf ∑15547 (9542)",
		                   "data": {
		                        "$area": 15547,
		                        "$dim": 15547,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/swing/plaf/windows",
		                   "name": "windows ∑145 (145)",
		                   "data": {
		                        "$area": 145,
		                        "$dim": 145,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/swing/plaf/synth",
		                   "name": "synth ∑5860 (5860)",
		                   "data": {
		                        "$area": 5860,
		                        "$dim": 5860,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/swing/table",
		                   "name": "table ∑368 (368)",
		                   "data": {
		                        "$area": 368,
		                        "$dim": 368,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/swing/icon",
		                   "name": "icon ∑121 (121)",
		                   "data": {
		                        "$area": 121,
		                        "$dim": 121,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/swing/text",
		                   "name": "text ∑1463 (1463)",
		                   "data": {
		                        "$area": 1463,
		                        "$dim": 1463,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/java2d",
		                   "name": "java2d ∑85074 (16410)",
		                   "data": {
		                        "$area": 85074,
		                        "$dim": 85074,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/java2d/xr",
		                   "name": "xr ∑8736 (8736)",
		                   "data": {
		                        "$area": 8736,
		                        "$dim": 8736,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/cmm",
		                   "name": "cmm ∑7412 (680)",
		                   "data": {
		                        "$area": 7412,
		                        "$dim": 7412,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/java2d/cmm/lcms",
		                   "name": "lcms ∑2990 (2990)",
		                   "data": {
		                        "$area": 2990,
		                        "$dim": 2990,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/cmm/kcms",
		                   "name": "kcms ∑3742 (3742)",
		                   "data": {
		                        "$area": 3742,
		                        "$dim": 3742,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/java2d/pipe",
		                   "name": "pipe ∑16036 (15675)",
		                   "data": {
		                        "$area": 16036,
		                        "$dim": 16036,
		                        "$color": "#80c080"
		                    },"children": [{
		"id": "sun/java2d/pipe/hw",
		                   "name": "hw ∑361 (361)",
		                   "data": {
		                        "$area": 361,
		                        "$dim": 361,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/java2d/loops",
		                   "name": "loops ∑13401 (13401)",
		                   "data": {
		                        "$area": 13401,
		                        "$dim": 13401,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/jules",
		                   "name": "jules ∑1951 (1951)",
		                   "data": {
		                        "$area": 1951,
		                        "$dim": 1951,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/pisces",
		                   "name": "pisces ∑12242 (12242)",
		                   "data": {
		                        "$area": 12242,
		                        "$dim": 12242,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/x11",
		                   "name": "x11 ∑2622 (2622)",
		                   "data": {
		                        "$area": 2622,
		                        "$dim": 2622,
		                        "$color": "#80c080"
		                    }},{
		"id": "sun/java2d/opengl",
		                   "name": "opengl ∑6264 (6264)",
		                   "data": {
		                        "$area": 6264,
		                        "$dim": 6264,
		                        "$color": "#80c080"
		                    }}]
		},{
		"id": "sun/security/rsa",
		                   "name": "security.rsa ∑2921 (2921)",
		                   "data": {
		                        "$area": 2921,
		                        "$dim": 2921,
		                        "$color": "#80c080"
		                    }}]
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
