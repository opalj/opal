/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * @author Alexander Appel
 */
function display(viewDetails) {
    // clear canvas
    svg.select("g").remove();
    table.selectAll("thead").remove();
    table.selectAll("tbody").remove();

    lastArgs = [viewDetails];

    let data;

    if (viewDetails !== undefined) {
        data = JSON.parse(provider.getSingleProject(viewDetails));
    } else {
        data = JSON.parse(provider.getSelectedProjects(0, 0, 1));
        clearNavItems();
    }

    // set navigation
    addNavItem("Selected Projects", "display", []);

    const color20 = d3.scaleOrdinal(d3.schemeCategory20);

    const zoom = d3.zoom();
    zoom.scaleExtent([1, 10]);
    // reset previous zoom if window got rescaled
    zoom.transform(svg, d3.zoomIdentity);
    svg.call(zoom);

    const pack = d3.pack()
        .size([width, height])
        .padding(1.5);

    // create hierarchy and set id field for display
    let idCount = 0;
    const root = d3.hierarchy(data)
        .sum(function(d) { return d.value; })
        .each(function(d) { d.id = idCount++;});

    // canvas for panning and zooming
    const canvas = svg.append("g");

    // create bubbles only with values > 0
    const node = canvas.selectAll(".node")
        .data(pack(root).leaves())
        .enter().append("g")
        .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

    node.append("circle")
        .attr("id", function(d) { return d.id; })
        .attr("r", function(d) { return d.r; })
        .style("fill", function(d) { return color20(d.id); })
        .on("click", function(d) {
            if (viewDetails !== undefined) return;
            addNavItem(d.data.id, "display", [d.data.id]);
            display(d.data.id);
        });

    node.append("clipPath")
        .attr("id", function(d) { return "clip-" + d.id; })
        .append("use")
        .attr("xlink:href", function(d) { return "#" + d.id; });

    node.append("text")
        .attr("id", "bubble-label")
        .attr("clip-path", function(d) { return "url(#clip-" + d.id + ")"; })
        .attr("display", function(d) { return data.options["Bubbles"] ? undefined : "none"; })
        .style("pointer-events", "none")
        .style("fill", function(d) { return getLabelContrast(color20(d.id)); })
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

    registerTooltip(node.selectAll("circle"), function(d, node) {
        node.append("span").text(d.data.id + "\n" + format(d.value));
    });

    // set sidebar display text
    d3.select("#show").select("span").text("Show Feature Values Zero");
    d3.select("#hide").select("span").text("Show Feature Values Zero");

    table.append("tbody").selectAll("tr")
        .data(root.leaves())
        .enter().filter(function(d) { return d.value == 0;})
        .append("tr").append("td")
            .style("width", "200px")
            .style("word-wrap", "break-word")
            .text(function(d) { return d.data.id; });

    let lastTransform = 1;
    // zoom event
    zoom.on("zoom", function(d) {
            canvas.attr("transform", d3.event.transform);

            // only check if scaling changed (this is for panning effects) 
            if (lastTransform !== d3.event.transform.k) {
                node.select("text")
                    .transition()
                    .style("font-size", fontSize / d3.event.transform.k + "px");

                node.selectAll("#bubble-label tspan").remove();
                node.selectAll("#bubble-label")
                    .selectAll("tspan")
                    .data(function(d) { return wrapText(d.data.id, d.r * 2, fontSize / d3.event.transform.k)})
                    .enter().append("tspan")
                        .attr("x", 0)
                        .attr("y", function(d, i, nodes) {
                            let offset = 0;
                            if (nodes.length > 1) {
                                offset = ((-fontSize * 0.5 / d3.event.transform.k) * (nodes.length / 2)) + (i * (fontSize / d3.event.transform.k));
                            } 
                            return offset;
                        })
                        .text(function(d) { return d; });
            }
            lastTransform = d3.event.transform.k;
    });
}