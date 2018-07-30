/* BSD 2-Clause License - see OPAL/LICENSE for details. */

/*
 * @author Alexander Appel
 */
function display() {
    // clear canvas
    svg.select("g").remove();
    table.selectAll("thead").remove();
    table.selectAll("tbody").remove();

    const data = JSON.parse(provider.getSelectedProjects(10, 0.015, 0.75));

    const color10 = d3.scaleOrdinal(d3.schemeCategory10);
    const color20 = d3.scaleOrdinal(d3.schemeCategory20);

    const zoom = d3.zoom();
    zoom.scaleExtent([1, 10]);
    // reset previous zoom if window got rescaled
    zoom.transform(svg, d3.zoomIdentity);
    svg.call(zoom);

    // bubble chart prepare
    const pack = d3.pack()
        .size([width, height])
        .padding(1.5);

    // pie chart prepare
    const pie = d3.pie()
        .sort(null)
        .value(function(d) { return d.value; });

    const arc = d3.arc().innerRadius(0);

    let idCount = 0;
    const legend = {};

    const root = d3.hierarchy(data)
        .sum(function(d) { return d.value;})
        .each(function(d) {
            d.id = idCount++;
            d.pieData = d.data.metrics;
        });

    // canvas for panning and zooming
    const canvas = svg.append("g");

    // defs for patterns and gradients
    canvas.append("defs");
        
    // create bubbles only with values > 0
    const node = canvas.selectAll("g.node")
        .data(pack(root).leaves())
        .enter().append("g")
        .filter(function(d) { return d.value > 0;})
        .attr("class", "node")
        .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

    const innerNode = node.selectAll("g.arc")
        .data(function(d) {
            return pie(d.pieData).map(function(m) {
                m.r = d.r;
                m.id = d.id;
                m.data.parent = d.data.id;
                return m;
            });
        })
        .enter().append("g");
        
    let colorIndex = 0;

    innerNode.append("path")
        .attr("d", function(d) {
            arc.outerRadius(d.r);
            return arc(d);
        })
        .attr("stroke-width", 1)
        .attr("stroke", function(d) {
            if (d.data.id === "<rest>") {
                return "grey";
            }
            return undefined;
        })
        .attr("fill", function(d, i) {
            if (d.data.id === "<rest>") {
                legend[d.data.id] = { type: "solid", color1: "#FFFFFF" };
            } else if (legend[d.data.id] === undefined) {
                // solid + gradient
                if (colorIndex < 20) {
                    legend[d.data.id] = { type: "solid", color1 : color20(colorIndex) };
                    colorIndex++;
                } else if (colorIndex < 25) {
                    legend[d.data.id] = { type: "gradient", color1: color10(colorIndex + 1), color2: color10(colorIndex + 2) };
                    colorIndex += 2;
                } else {
                    legend[d.data.id] = { type: "gradient", color2: color10(colorIndex + 1), color1: color10(colorIndex + 2) };
                    colorIndex += 2;
                }
            }

            if (legend[d.data.id].type === "solid") {
                return legend[d.data.id].color1;
            } else if (legend[d.data.id].type === "gradient") {
                const gradId = d.id + "_" + i;
                const gradient = canvas.select("defs").append("radialGradient")
                    .attr("gradientUnits", "userSpaceOnUse")
                    .attr("cx", 0)
                    .attr("cy", 0)
                    .attr("r", d.r)
                    .attr("id", "grad_" + gradId);
                gradient.append("stop").attr("offset", "30%").style("stop-color", legend[d.data.id].color1);
                gradient.append("stop").attr("offset", "70%").style("stop-color", legend[d.data.id].color2);
                return "url(#grad_" + gradId + ")";
            }
            console.error("Unknown color type during color assignment of " + d.data.id + " in pie chart. Should be 'solid' or 'gradient'.");
            return "#FFFFFF";
        });

    innerNode.append("text")
        .attr("transform", function(d) { arc.outerRadius(d.r); return "translate(" + arc.centroid(d) + ")"; })
        .style("text-anchor", "middle")
        .style("fill", function(d) { return getLabelContrast(legend[d.data.id].color1); })
        .style("pointer-events", "none")
        .text(function(d) { return d.data.id; })
        .each(function(d) { d.visible = isElementInArc(d, arc, this); })
        .style("visibility", function (d) { return d.visible && data.options["Pies"] ? "visible" : "hidden"; });

    registerTooltip(innerNode.selectAll("path"), function(d, node) {
        if (d.data.metrics !== undefined) {
            node.append("span").text(d.data.id + "\n Total: " + format(d.value) + "\n\n");
            const table = node.append("table");
            for (var item of d.data.metrics) {
                const column = table.append("tr");
                column.append("td").text(item.id);
                column.append("td").text(format(item.value));
            }
        } else {
            node.append("span").text(d.data.id + "\n" + format(d.value));
        }
    });

    node.append("circle")
        .attr("id", function(d) { return d.id; })
        .attr("r", function(d) { return d.r; })
        .style("fill", "none");

    node.append("clipPath")
        .attr("id", function(d) { return "clip-" + d.id; })
        .append("use")
        .attr("xlink:href", function(d) { return "#" + d.id; });

    node.append("text")
        .attr("id", "bubble-label")
        .attr("clip-path", function(d) { return "url(#clip-" + d.id + ")"; })
        .attr("display", function(d) { return data.options["Bubbles"] ? undefined : "none"; })
        .style("pointer-events", "none")
        .selectAll("tspan")
        .data(function(d) { return wrapText(d.data.id, d.r * 2, fontSize)})
        .enter().append("tspan")
            .attr("x", 0)
            .attr("y", function(d, i, nodes) {
                let offset = 0;
                if (nodes.length > 1) {
                    offset = (-fontSize * 0.5 * (nodes.length / 2)) + (i * fontSize);
                }
                return offset;
            })
            .text(function(d) { return d; });

    // set sidebar display text
    const colorCount = Object.keys(legend).length;
    d3.select("#show").select("span").text("Color Keys <" + colorCount + ">");
    d3.select("#hide").select("span").text("Color Keys <" + colorCount + ">");

    // create table displaying color keys
    //pack and display legend
    const packed = [];
    for (var key of Object.keys(legend).sort(function (a, b) {
        return a.toLowerCase().localeCompare(b.toLowerCase());
    })) {
        if (legend[key].type === "solid") {
            packed.push({id: key, value: legend[key].color1});
        } else if (legend[key].type === "gradient") {
            const gradId = packed.length;
            const gradient = canvas.select("defs").append("linearGradient")
                .attr("id", "grad_" + gradId);
            gradient.append("stop").attr("offset", "30%").style("stop-color", legend[key].color1);
            gradient.append("stop").attr("offset", "70%").style("stop-color", legend[key].color2);
            packed.push({id: key, value: "url(#grad_" + gradId + ")"});
        } else {
            console.error("Unknown color type during color legend building. Should be 'solid' or 'gradient'.");
        }
    }

    const cell = table.append("tbody").selectAll("tr")
        .data(packed)
        .enter().append("tr")
            .append("td")
                .style("width", "200px")
                .style("word-wrap", "break-word");
            
    cell.append("svg")
            .attr("width", 10)
            .attr("height", 10)
        .append("rect")
            .attr("width", 10)
            .attr("height", 10)
            .attr("stroke-width", 0.5)
            .attr("stroke", "black")
            .attr("fill", function(d) { return d.value; });

    cell.append("text")
        .style("margin-left", "3px")
        .text(function(d) { return d.id });

    
    let lastTransform = 1;
    // zoom event
    zoom.on("zoom", function(d) {
            canvas.attr("transform", d3.event.transform);

            // only check if scaling changed (this is for panning effects) 
            if (lastTransform !== d3.event.transform.k) {
                node.selectAll("text")
                    .transition()
                    .style("font-size", fontSize / d3.event.transform.k + "px");

                node.selectAll("g.arc").select("text")
                    .each(function(d) { d.visible = isElementInArc(d, arc, this); })
                    .style("visibility", function (d) { return d.visible && data.options["Pies"] ? "visible" : "hidden"; });

                node.selectAll("#bubble-label tspan").remove();
                node.selectAll("#bubble-label")
                    .selectAll("tspan")
                    .data(function(d) { return wrapText(d.data.id, d.r * 2, fontSize / d3.event.transform.k)})
                    .enter().append("tspan")
                        .attr("x", 0)
                        .attr("y", function(d, i, nodes) {
                            var offset = 0;
                            if (nodes.length > 1) {
                                offset = ((-fontSize * 0.5 / d3.event.transform.k) * (nodes.length / 2)) + (i * (fontSize / d3.event.transform.k));
                            } 
                            return offset;
                        })
                        .text(function(d) { return d; });

                innerNode.select("path")
                        .attr("stroke-width", function (d) { return 1 / d3.event.transform.k; });
            }
            lastTransform = d3.event.transform.k;
    });
}