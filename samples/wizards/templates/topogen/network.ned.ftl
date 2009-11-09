<@setoutput file=newFileName?default("")/>
${bannerComment}

// Created: ${date} for project ${projectName}

<#if nedPackageName!="">package ${nedPackageName};</#if>

<#if generateNodeTypeDecl>
module ${nodeType} {
    parameters:
        @display("i=abstract/router_vs");
    gates:
        inout g[];
}
</#if>

<#if generateChannelTypeDecl>
channel ${channelType} extends ned.DatarateChannel {
    parameters:
        int cost = default(0);
}
</#if>

<#assign numNodes = numNodes?number>
<#assign numLinks = numLinks?number>
<#assign seed = seed?number>
<#assign param1 = param1Times100?number / 100.0>
<#assign param2 = param2Times100?number / 100.0>

<#assign topo = LangUtils.newInstance("org.example.Topogen")>
<#assign dummy = topo.setNodes(numNodes) ?default(0)>
<#assign dummy = topo.setEdges(numLinks) ?default(0)>
<#assign dummy = topo.setSeed(seed) ?default(0)>
<#assign dummy = topo.setParam1(param1) ?default(0)>
<#assign dummy = topo.setParam2(param2) ?default(0)>
<#assign neighborLists = topo.generate()>

//
// Graph generated by Topogen (${numNodes} nodes, ${numLinks} edges, seed=${seed}).
//
// Edge list:
// <pre>
// ${topo.toString().trim().replace("\n", "\n// ")}
// </pre>
//
network ${nedTypeName} {
    submodules:
<#list 0..neighborLists.size()-1 as i>
        node${i} : ${nodeType};
</#list>
    connections:
<#list 0..neighborLists.size()-1 as i>
  <#assign neighborList = neighborLists[i] >
  <#list neighborList as neighbor>
     <#if (i < neighbor)>
        node${i}.g++ <--> ${channelType} <--> node${neighbor}.g++;
     </#if>
  </#list>
</#list>
}
