function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    console.log('Query variable %s not found', variable);
}
    
    
var standardLabel = d3.format(".2p") // only 2 decimal places

// make it directly from the json object     
function makeHistogramFromJSON(targetObj,inw,inh) { 
  return function(error, data) {
    var width = inw || 900 ,
        height = inh || 300,
        pad = 20, 
        left_pad = 100;
    
    var x = d3.scale.ordinal().rangeRoundBands([left_pad, width-pad], 0.1);
    var y = d3.scale.linear().range([height-pad, pad]);

    var xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(d3.format(".4g"));
    var yAxis = d3.svg.axis().scale(y).orient("left");

    var svg = d3.select("#"+targetObj)
        .append("svg")
        .attr("width", width)
        .attr("height", height);
    // add an index column
    var i=0;
    data.map(function(ele) {ele["index"]=i++})
    
    x.domain(data.map(function (d) { return d.bin; }));
    y.domain([0, d3.max(data, function (d) { return d.count; })]);

    svg.append("g")
        .attr("class", "axis")
        .attr("transform", "translate(0, "+(height-pad)+")")
        .call(xAxis);

    svg.append("g")
        .attr("class", "axis")
        .attr("transform", "translate("+(left_pad-pad)+", 0)")
        .call(yAxis);

    svg.selectAll('rect')
        .data(data)
        .enter()
        .append('rect')
        .attr('class', 'bar')
        .attr('x', function (d) { return x(d.bin); })
        .attr('width', x.rangeBand())
        .attr('y', height-pad)
        .transition()
        .delay(function (d) { return d.index*20; })
        .duration(800)
        .attr('y', function (d) { return y(d.count); })
        .attr('height', function (d) { return height-pad - y(d.count); });
}        
}
// make it from a url
function makeHistogramFromURL(jsonUrl,targetObj) {
    d3.json(jsonUrl, makeHistogramFromJSON(targetObj));
}

