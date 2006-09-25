<%--
  Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable.
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ page import="org.apache.geronimo.console.util.PortletManager" %>
<portlet:defineObjects/>

<!------------------------>
<!--     DOJO Stuff     -->
<!------------------------>

<%
    // JMX icon
    String consoleFrameworkContext = PortletManager.getConsoleFrameworkServletPath(request);
    String jmxIconURI = consoleFrameworkContext + "/../images/ico_filetree_16x16.gif";
%>

<style type="text/css">
<!-- Splitter styles -->
body .dojoHtmlSplitterPanePanel {
	background: white;
	overflow: auto;
}

<!-- Sortable table styles -->
table {
	font-family:Lucida Grande, Verdana;
	font-size:0.8em;
	width:100%;
	cursor:default;
}

* html div.tableContainer {	/* IE only hack */
	width:95%;
	border:1px solid #ccc;
	height: 285px;
	overflow-x:hidden;
	overflow-y: auto;
}

table td, table th {
	/* border-right:1px solid #999; */
	/* padding:2px; */
	font-weight: normal;
}

table thead td, table thead th { 
	background: #2581C7; /* #94BEFF */
	color: #FFFFFF;	/* added */
}

* html div.tableContainer table thead tr td,
* html div.tableContainer table thead tr th {
	/* IE Only hacks */
	position:relative;
	top:expression(dojo.html.getFirstAncestorByTag(this,'table').parentNode.scrollTop-2);
}

html>body tbody.scrollContent {
	height: 100%; /* 262px */
	overflow-x: hidden;
	overflow-y: hidden; /* auto */
}

tbody.scrollContent td, tbody.scrollContent tr td {
	background: #FFF;
	padding: 2px;
}

tbody.scrollContent tr.alternateRow td {
	background: #F2F2F2; /* #e3edfa */
	padding: 2px;
}

tbody.scrollContent tr.selected td {
	background: yellow;
	padding: 2px;
}

tbody.scrollContent tr:hover td {
	background: #a6c2e7;
	padding: 2px;
}

tbody.scrollContent tr.selected:hover td {
	background: #ff3;
	padding: 2px;
}
</style>

<script>
/**
 * Global vars 
 */
var _selectedNode = null;  // Selected tree node
var _attribValueID = null; // ID of the attribute to update in the Attributes table
var _attribValue = null;   // Value of the attribute to update in the Attributes tabl

/**
 * Get selected node 
 */
function getSelectedNode() {
    var tree = dojo.widget.byId('jmxTree');
    var selectedNode = tree.selector.selectedNode;
    return selectedNode;
}

/**
 * Set the mouse pointer (NOT USED)
 */
function setPointer(cursor) {
    if (document.all) {
        // Solution 1
        // for (var i = 0; i < document.all.length; i++) {
        //     document.all(i).style.cursor = cursor;
        // }
        
        // Solution 2
        // document.all('mainLayout').style.cursor = cursor;
        // document.getElementById('mainLayout').style.cursor = cursor;

        $('mainLayout').style.cursor = cursor;
        $('rootfragment').style.cursor = cursor;
    }
}

/**
 * Dojo init stuff 
 */
dojo.addOnLoad(
    function() {
        var treeController = dojo.widget.byId('treeController');

        /**
         * Tree click event handler (expand & contract nodes)
         */
        dojo.event.connect(
            'before',
            treeController,
            'onTreeClick',
            {
                beforeTreeClick: function(evt) {
                    var selectedNode = evt.source;
                    if ((selectedNode.state == 'UNCHECKED') && (selectedNode.isExpanded == false)) {
                        _selectedNode = selectedNode;
                        // Check if it's 'searchMBeans'
                        if ((selectedNode.widgetId == 'searchMBeans') && (selectedNode.children.length == 0)) {
                            // skip DWR call
                            selectedNode.state = 'LOADED';
                            return;
                        } else {
                            if (selectedNode.widgetId.indexOf(':') != -1) {
                                // Do search by pattern
                                JMXHelper.listByPattern(<portlet:namespace/>updateJMXTree, _selectedNode.widgetId);
                            } else {
                                // Do search by type
                                JMXHelper.listByJ2EEType(<portlet:namespace/>updateJMXTree, _selectedNode.widgetId);
                            }
                        }
                    }
                }
            },
            'beforeTreeClick'
        );

		/**
		 * Tree node title click event handler 
		 */
		var tree = dojo.widget.byId('jmxTree');
		dojo.event.topic.subscribe(
			tree.eventNames.titleClick,
			function(message) {
			    var abstractName = message.source.widgetId;
			    if (abstractName.indexOf('::') == -1) {
			        // No marker means not an abstract name, clear tables
			        DWRUtil.removeAllRows('basicInfoTableBody');
			        DWRUtil.removeAllRows('attributesTableBody');
			        DWRUtil.removeAllRows('operationsTableBody');
			    } else {
    			    // Remove marker to get abstract name
    			    abstractName = abstractName.substring(abstractName.indexOf('::') + 2);
    			    JMXHelper.getMBeanInfo(<portlet:namespace/>updateBasicInfoTable, abstractName);
    			    JMXHelper.getAttributes(<portlet:namespace/>updateAttributesTable, abstractName);
    			    JMXHelper.getOperations(<portlet:namespace/>updateOperationsTable, abstractName);
    			}
			}
		);

        /**
         * Tree context menu event handler: 'Refresh' (NOT USED)
         */
		dojo.event.topic.subscribe(
		    'treeContextMenuRefresh/engage',
			function (menuItem) {
			    var selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    alert('Please select a tree node.');
                    return;
                }
			    if ((selectedNode.state == 'UNCHECKED') && (selectedNode.isExpanded == false)) {
			        // Unchecked tree node, do nothing
                } else {
                    // Remove children
                    var treeController = dojo.widget.byId('treeController');
    			    var children = selectedNode.children;
    			    while (children.length > 0) {
                        var node = children[0];
                        treeController.removeNode(node);
                        node.destroy();
    			    }
    			    // Add children
    			    _selectedNode = selectedNode;
    			    // TODO: Insert add tree node children code here
    	        }
            }
		);

        /**
         * Tree context menu event handler: 'Search...' 
         */
		dojo.event.topic.subscribe(
		    'treeContextMenuSearch/engage',
			function (menuItem) {
			    var selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    alert('Please select a tree node.');
                    return;
                }
			    var mainTabContainer = dojo.widget.byId('mainTabContainer');
			    var searchTab = dojo.widget.byId('searchTab');
			    mainTabContainer.selectTab(searchTab);
			}
		);

        /**
         * Tree context menu event handler: 'View Attributes'
         */
		dojo.event.topic.subscribe(
		    'treeContextMenuViewAttribs/engage',
			function (menuItem) {
			    var selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    alert('Please select a tree node.');
                    return;
                }
			    var mainTabContainer = dojo.widget.byId('mainTabContainer');
			    var attributesTab = dojo.widget.byId('attributesTab');
			    mainTabContainer.selectTab(attributesTab);
            }
		);

        /**
         * Tree context menu event handler: 'View Operations' 
         */
		dojo.event.topic.subscribe(
		    'treeContextMenuViewOps/engage',
			function (menuItem) {
			    var selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    alert('Please select a tree node.');
                    return;
                }
			    var mainTabContainer = dojo.widget.byId('mainTabContainer');
			    var operationsTab = dojo.widget.byId('operationsTab');
			    mainTabContainer.selectTab(operationsTab);
            }
		);

        /**
         * Tree context menu event handler: 'View Info' 
         */
		dojo.event.topic.subscribe(
		    'treeContextMenuViewInfo/engage',
			function (menuItem) {
			    var selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    alert('Please select a tree node.');
                    return;
                }
			    var mainTabContainer = dojo.widget.byId('mainTabContainer');
			    var infoTab = dojo.widget.byId('infoTab');
			    mainTabContainer.selectTab(infoTab);
            }
		);
    }
);

/**
 * Search button clicked event handler 
 */
function searchBtnClicked() {
    var jmxQuery = dojo.widget.byId('jmxQuery').getValue();
    JMXHelper.listByPattern(<portlet:namespace/>updateSearchMBeansTreeNode, jmxQuery);
}
</script>

<!----------------------->
<!--     DWR Stuff     -->
<!----------------------->

<% String dwrForwarderServlet = PortletManager.getConsoleFrameworkServletPath(request) + "/../dwr"; %>
<script type='text/javascript' src='<%= dwrForwarderServlet %>/interface/JMXHelper.js'></script>
<script type='text/javascript' src='<%= dwrForwarderServlet %>/engine.js'></script>
<script type='text/javascript' src='<%= dwrForwarderServlet %>/util.js'></script>

<script>
/**
 * Sync calls 
 */
DWREngine.setAsync(false);

/**
 * Generic error handler 
 */
DWREngine.setErrorHandler(
    function (errorString) {
        alert('Error: ' + errorString);
    }
);

/**
 * DWR table render option
 */
var tableOption = {
    rowCreator: function(options) {
        var row = document.createElement('tr');
        return row;
    },
    cellCreator: function(options) {
        var td = document.createElement('td');
        if ((options.rowIndex % 2) == 0) {
            td.style.backgroundColor = '#FFFFFF';
        } else {
            td.style.backgroundColor = '#F2F2F2';
        }
        return td;
    }
}

/**
 * Update MBean attributes table 
 */
function <portlet:namespace/>updateAttributesTable(attributes) {
    DWRUtil.removeAllRows('attributesTableBody');
    DWRUtil.addRows(
        'attributesTableBody', 
        attributes,
        [ 
            function(attribute) { /* AttribName Column */
                return attribute['name']; 
            },
            function(attribute) { /* AttribType Column */
                return attribute['type']; 
            },
            function(attribute) { /* AttribValue  Column */
                var attribValue = attribute['value'];
                var attribType = attribute['type'];
                var isWritable = attribute['writable'];
                var attribSetterName = attribute['setterName'];
                var attribValueID = attribSetterName + '_value';
                if (isWritable == 'true') {
                    // OK: attribValue = "<input type='text' id='" + attribValueID + "' value='" + attribValue + "' style='width: 300px;' disabled/>";
                    attribValue = "<div id='" + attribValueID + "'>" + attribValue + "</div>";       
                    return attribValue;
                }
                return attribValue;
            },
            function(attribute) { /* AttribGetterName Column */
                return attribute['getterName']; 
            },
            function(attribute) { /* AttribSetterName Column */
                var attribSetterName = attribute['setterName']; 
                var abstractName = $('abstractName').value;
                var attribName = attribute['name'];
                var attribType = attribute['type'];
                var attribValueID = attribSetterName + '_value';
                var isWritable = attribute['writable'];
                if (isWritable == 'true') {
                    attribSetterName = 
                        "<div align='center'>" +
                        "<input type='button' value='" + attribSetterName + "' " + 
                        "onclick='setAttribFN(\"" + abstractName + "\", \"" + attribName + "\", DWRUtil.getValue(\"" + attribValueID + "\"), \"" + attribValueID + "\", \"" + attribType + "\")' " +
                        "/>&nbsp;" + 
                        "</div>";
                    return attribSetterName;
                }
                return attribSetterName; 
            },
            function(attribute) { /* AttribManageable Column */
                return attribute['manageable']; 
            },
            function(attribute) { /* AttribPersistent Column */
                return attribute['persistent']; 
            },
            function(attribute) { /* AttribReadable Column */
                return attribute['readable']; 
            },
            function(attribute) { /* AttribWritable Column */
                return attribute['writable']; 
            }
        ],
        tableOption
    );

    // Render sortable table
	var tbl = dojo.widget.byId("attribsTable");
	if (tbl) {
	    tbl.render(false);
	}
}

/**
 * Update MBean operations table 
 */
function <portlet:namespace/>updateOperationsTable(operations) {
    DWRUtil.removeAllRows('operationsTableBody');
    DWRUtil.addRows(
        'operationsTableBody', 
        operations,
        [
            function(operation) { /* OperName  Column */
                var abstractName = $('abstractName').value;
                var opName = operation['name'];
                var params = operation['parameterList'];
                var paramSize = params.length;
                var oper = 
                    "<div align='right'>" +
                    "<input type='button' value='" + opName + "' " + 
                    "onclick='invokeOperFN(\"" + abstractName + "\", \"" + opName + "\", " + paramSize + ")' " +
                    "/>&nbsp;" + 
                    "</div>";
                return oper;
            },
            function(operation) { /* OperParameterList Column */
                // TODO: Fix name collision problem for overloaded operations with same number of parameters
                var opName = operation['name'];
                var params = operation['parameterList'];
                var paramList = "";
                if (params.length == 0) {
                    paramList = "<b>()</b>";
                } else {
                    paramList = "<b>(&nbsp;</b>";
                    for (var i = 0; i < params.length; i++) {
                        var opParamValueID = opName + '_paramValue' + (i+1) + '.' + params.length;
                        var opParamTypeID = opName + '_paramType' + (i+1) + '.' + params.length;
                        paramList += "<span id='" + opParamTypeID + "'>" + params[i] + "</span>&nbsp;<input type='text' id='" + opParamValueID + "' style='width: 100px;'/>";
                        if ((i+1) < params.length) {
                            paramList += "<b>&nbsp;,&nbsp;</b>";
                        }
                    }
                    paramList += "<b>&nbsp;)</b>";
                }
                return paramList; 
            }
        ],
        tableOption
    );
}

/**
 * Update MBean basic info table
 */
function <portlet:namespace/>updateBasicInfoTable(basicInfo) {
    DWRUtil.removeAllRows('basicInfoTableBody');
    DWRUtil.addRows(
        'basicInfoTableBody', 
        basicInfo,
        [
            function(info) { /* BasicInfoName Column */
                var name = "<div align='right'>" + info[0] + ":&nbsp;</div>";
                return name;
            },
            function(info) { /* BasicInfoValue Column */
                var value = info[1] + "<input type='hidden' id='" + info[0] + "' value='" + info[1] + "'>";
                return value;
            }
        ],
        tableOption
    );
}

/**
 * Update 'Search MBeans' tree node 
 */
function <portlet:namespace/>updateSearchMBeansTreeNode(searchResult) {
    // Remove nodes
    var searchMBeansNode = dojo.widget.byId('searchMBeans');
    var treeController = dojo.widget.byId('treeController');
    var children = searchMBeansNode.children;

    while (children.length > 0) {
        var node = children[0];
        treeController.removeNode(node);
        node.destroy();
    }
    treeController.removeNode(searchMBeansNode); // This fixed the layout problem
    searchMBeansNode.destroy();
    
    // Add nodes
    nodeTitle = 'Search MBeans (' + searchResult.length + ' matches)';
    searchMBeansNode = dojo.widget.createWidget(
        'TreeNode', 
        {title: nodeTitle, widgetId: 'searchMBeans', isFolder: true, childIconSrc:'<%= jmxIconURI %>', actionsDisabled: ['view']}
    );
    var tree = dojo.widget.byId('jmxTree');
	tree.addChild(searchMBeansNode); 
    for (i = 0; i < searchResult.length; i++) {
        var entry = searchResult[i];
        var id = searchMBeansNode.widgetId + '::' + entry[0]; // make it unique
        var newNode = dojo.widget.createWidget(
            'TreeNode', 
            {title: entry[1], widgetId: id, label: entry, isFolder: false, childIconSrc:'<%= jmxIconURI %>'}
        );
		searchMBeansNode.addChild(newNode);        
    }
    searchMBeansNode.state = 'LOADED';

    // Exapand node
    if (searchMBeansNode.isExpanded == false) {
        treeController.expandToLevel(searchMBeansNode, 1);
    }
    
    // Select node
    var treeSelector = dojo.widget.byId("treeSelector");
    if (getSelectedNode() != null) {
        treeSelector.deselect();
    }
    treeSelector.doSelect(searchMBeansNode);
}

/**
 * Update JMX tree
 */
function <portlet:namespace/>updateJMXTree(entries) {
    for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        var id = _selectedNode.widgetId + '::' + entry[0]; // make it unique
        var newNode = dojo.widget.createWidget(
            'TreeNode', 
            {title: entry[1], widgetId: id, label: entry[1], isFolder: false, childIconSrc:'<%= jmxIconURI %>'}
        );
		_selectedNode.addChild(newNode);
    }
    _selectedNode.state = 'LOADED';
}

/**
 * Set Attribute Function 
 */
function setAttribFN(abstractName, attribName, attribValue, attribValueID, attribType) {
    _attribValueID = attribValueID;
    var newValue = prompt("Enter new value for attribute '" + attribName + "':", attribValue);
    if (newValue == null) {
        // Do nothing.
    } else {
        // Check boolean case
        if ((attribType == 'boolean') || (attribType == 'java.lang.Boolean')) {
            if (newValue != 'true') {
                newValue = 'false';
            }
        }
        _attribValue = newValue;
        // Set attribute
        JMXHelper.setAttribute(
            function(result) { /* setAttribFN Callback */
                if (result[1] == '<SUCCESS>') {
                    alert("Attribute '" + result[0] + "' successfully set."); 
                    DWRUtil.setValue(_attribValueID, _attribValue); // update table cell
                } else {
                    alert("Failed to set attribute '" + result[0] + "': " + result[1]);
                }
            },
            abstractName, attribName, newValue, attribType /* Arguments */
        );
    }
}

/**
 * Invoke Operation Function 
 */
function invokeOperFN(abstractName, opName, paramSize) {
    if (paramSize == 0) {
        // Operation without parameters
        // Invoke operator with no args        
        JMXHelper.invokeOperNoArgs(
            function(result) { /* invokeOperNoArgs Callback */
                alert(result[0] + ' returned: ' + result[1]);
            },
            abstractName, opName /* Arguments */
        );
    } else {
        // Operation with parameters
        var paramValues = new Array(paramSize);
        var paramTypes = new Array(paramSize);
        for (var i = 0; i < paramSize; i++) {
            var opParamValueID = opName + '_paramValue' + (i+1) + '.' + paramSize;
            var opParamTypeID = opName + '_paramType' + (i+1) + '.' + paramSize;
            paramValues[i] = DWRUtil.getValue(opParamValueID);
            paramTypes[i] = DWRUtil.getValue(opParamTypeID);
        }
        // Invoke operator with args
        JMXHelper.invokeOperWithArgs(
            function(result) { /* invokeOperWithArgs Callback */
                alert(result[0] + ' returned: ' + result[1]);
            },
            abstractName, opName, paramValues, paramTypes
        );
    }
}

/**
 * Prints 'LOADING' message while waiting for DWR method calls 
 */
function init() {
    // DWRUtil.useLoadingMessage();
}

/**
 * Call on DWR load
 */
function callOnLoad(load) {
    if (window.addEventListener) {
        window.addEventListener('load', load, false);
    } else if (window.attachEvent) {
        window.attachEvent('onload', load);
    } else {
        window.onload = load;
    }
}

/**
 * Call init function
 */
callOnLoad(init);
</script>

<div dojoType="TreeContextMenu" toggle="explode" contextMenuForWindow="false" widgetId="treeContextMenu">
	<!-- <div dojoType="TreeMenuItem" treeActions="refreshNode" widgetId="treeContextMenuRefresh" caption="Refresh"></div> -->
	<div dojoType="TreeMenuItem" treeActions="searchNode" widgetId="treeContextMenuSearch" caption="Search..."></div>
	<div dojoType="TreeMenuItem" treeActions="view" widgetId="treeContextMenuViewAttribs" caption="View Attributes"></div>
	<div dojoType="TreeMenuItem" treeActions="view" widgetId="treeContextMenuViewOps" caption="View Operations"></div>
	<div dojoType="TreeMenuItem" treeActions="view" widgetId="treeContextMenuViewInfo" caption="View Info"></div>
</div>

<div dojoType="TreeSelector" widgetId="treeSelector"></div>
<div dojoType="TreeBasicController" widgetId="treeController"></div>

<!-- Main layout container -->
<div dojoType="LayoutContainer"
	layoutChildPriority='left-right'
	id="mainLayout"
	style="height: 700px;">

    <!-- Horizontal split container -->
	<div dojoType="SplitContainer"
		orientation="horizontal"
		sizerWidth="5"
		activeSizing="1"
		layoutAlign="client">

        <!-- JMX tree -->
        <div dojoType="Tree"
            toggle="fade"
            layoutAlign="flood"
			sizeMin="60"
			sizeShare="40"
            widgetId="jmxTree"
            selector="treeSelector"
            controller="treeController"
            expandLevel="0"
            menu="treeContextMenu"
            strictFolders="false">

            <!-- All MBeans -->
            <div dojoType="TreeNode" title="All MBeans" widgetId="allMBeans" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view">
         	    <div dojoType="TreeNode" title="geronimo" widgetId="geronimo:*" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="geronimo.config" widgetId="geronimo.config:*" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
            </div>

            <!-- J2EE MBeans -->
         	<div dojoType="TreeNode" title="J2EE MBeans" widgetId="j2eeMBeans" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view">
         	    <div dojoType="TreeNode" title="AppClientModule" widgetId="AppClientModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="EJBModule" widgetId="EJBModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="EntityBean" widgetId="EntityBean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="J2EEApplication" widgetId="J2EEApplication" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="J2EEDomain" widgetId="J2EEDomain" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="J2EEServer" widgetId="J2EEServer" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JavaMailResource" widgetId="JavaMailResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAConnectionFactory" widgetId="JCAConnectionFactory" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAManagedConnectionFactory" widgetId="JCAManagedConnectionFactory" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAResource" widgetId="JCAResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JDBCDataSource" widgetId="JDBCDataSource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JDBCDriver" widgetId="JDBCDriver" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JDBCResource" widgetId="JDBCResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JMSResource" widgetId="JMSResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JNDIResource" widgetId="JNDIResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JTAResource" widgetId="JTAResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JVM" widgetId="JVM" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="MessageDrivenBean" widgetId="MessageDrivenBean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ResourceAdapter" widgetId="ResourceAdapter" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ResourceAdapterModule" widgetId="ResourceAdapterModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="RMI_IIOPResource" widgetId="RMI_IIOPResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="Servlet" widgetId="Servlet" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="StatefulSessionBean" widgetId="StatefulSessionBean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="StatelessSessionBean" widgetId="StatelessSessionBean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="URLResource" widgetId="URLResource" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="WebModule" widgetId="WebModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	</div>
         	
         	<!-- Geronimo MBeans -->
         	<div dojoType="TreeNode" title="Geronimo MBeans" widgetId="geronimoMBeans" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view">
         	    <div dojoType="TreeNode" title="AppClient" widgetId="AppClient" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ArtifactManager" widgetId="ArtifactManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ArtifactResolver" widgetId="ArtifactResolver" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="AttributeStore" widgetId="AttributeStore" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ConfigBuilder" widgetId="ConfigBuilder" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ConfigurationEntry" widgetId="ConfigurationEntry" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ConfigurationManager" widgetId="ConfigurationManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ConfigurationStore" widgetId="ConfigurationStore" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="CORBABean" widgetId="CORBABean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="CORBACSS" widgetId="CORBACSS" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="CORBATSS" widgetId="CORBATSS" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="Deployer" widgetId="Deployer" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="DeploymentConfigurer" widgetId="DeploymentConfigurer" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="GBean" widgetId="GBean" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="Host" widgetId="Host" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JaasLoginService" widgetId="JaasLoginService" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JACCManager" widgetId="JACCManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JAXRConnectionFactory" widgetId="JAXRConnectionFactory" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAActivationSpec" widgetId="JCAActivationSpec" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAAdminObject" widgetId="JCAAdminObject" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAConnectionManager" widgetId="JCAConnectionManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAConnectionTracker" widgetId="JCAConnectionTracker" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAResourceAdapter" widgetId="JCAResourceAdapter" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JCAWorkManager" widgetId="JCAWorkManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JMSConnector" widgetId="JMSConnector" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JMSPersistence" widgetId="JMSPersistence" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="JMSServer" widgetId="JMSServer" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="KeyGenerator" widgetId="KeyGenerator" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="Keystore" widgetId="Keystore" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="LoginModule" widgetId="LoginModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="LoginModuleUse" widgetId="LoginModuleUse" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="MEJB" widgetId="MEJB" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ModuleBuilder" widgetId="ModuleBuilder" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="PersistentConfigurationList" widgetId="PersistentConfigurationList" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="RealmBridge" widgetId="RealmBridge" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="Repository" widgetId="Repository" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="RoleMapper" widgetId="RoleMapper" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="SecurityRealm" widgetId="SecurityRealm" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ServiceModule" widgetId="ServiceModule" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ServletTemplate" widgetId="ServletTemplate" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ServletWebFilterMapping" widgetId="ServletWebFilterMapping" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="ServletWebServiceTemplate" widgetId="ServletWebServiceTemplate" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="SystemLog" widgetId="SystemLog" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="TomcatValve" widgetId="TomcatValve" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="TransactionContextManager" widgetId="TransactionContextManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="TransactionLog" widgetId="TransactionLog" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="TransactionManager" widgetId="TransactionManager" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="URLPattern" widgetId="URLPattern" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="URLWebFilterMapping" widgetId="URLWebFilterMapping" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="WebFilter" widgetId="WebFilter" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="WSLink" widgetId="WSLink" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="XIDFactory" widgetId="XIDFactory" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="XIDImporter" widgetId="XIDImporter" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="XmlAttributeBuilder" widgetId="XmlAttributeBuilder" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
         	    <div dojoType="TreeNode" title="XmlReferenceBuilder" widgetId="XmlReferenceBuilder" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view"></div>
            </div>
            
            <!-- Search MBeans -->
         	<div dojoType="TreeNode" title="Search MBeans" widgetId="searchMBeans" isFolder="true" childIconSrc="<%= jmxIconURI %>" actionsDisabled="view">
         	</div>
        </div> <!-- JMX tree -->

        <!-- Main tab container -->
    	<div id="mainTabContainer" 
    	    dojoType="TabContainer" 
    	    selectedTab="attributesTab" 
    	    style="overflow: hidden" 
    	    sizeShare="60">
            
            <!-- Attributes tab -->
    		<div id="attributesTab" dojoType="ContentPane" title="MBean Attributes" label="Attributes">
                <br>
            	<table dojoType="SortableTable" 
            	    widgetId="attribsTable" 
            	    tbodyClass="scrollContent" 
            	    enableMultipleSelect="true" 
            	    enableAlternateRows="true" 
            	    rowAlternateClass="alternateRow" 
            	    cellpadding="0" 
            	    cellspacing="2" 
            	    border="0"
            	    width="100%">
                    <thead>
                        <tr>
                            <th field="Name" dataType="String" width="10%">&nbsp;Name&nbsp;</th>
                            <th field="Type" dataType="String" width="10%">&nbsp;Type&nbsp;</th>
                            <th dataType="html" width="20%">&nbsp;Value&nbsp;</th>
                            <th field="Getter" dataType="String" width="10%">&nbsp;Getter&nbsp;</th>
                            <th dataType="html" width="10%">&nbsp;Setter&nbsp;</th>
                            <th field="Manageable" dataType="String" width="10%">&nbsp;Manageable&nbsp;</th>
                            <th field="Persistent" dataType="String" width="10%">&nbsp;Persistent&nbsp;</th>
                            <th field="Readable" dataType="String" width="10%">&nbsp;Readable&nbsp;</th>
                            <th field="Writable" dataType="String" width="10%">&nbsp;Writable&nbsp;</th>
                        </tr>
                    </thead>
                    <tbody id="attributesTableBody">
                    </tbody>
                </table>
    		</div> <!-- Attributes tab -->

            <!-- Operations tab -->
    		<div id="operationsTab" dojoType="ContentPane" title="MBean Operations" label="Operations">
                <br>
                <table width="100%">
                    <tr>
                        <!--
                        <td class="DarkBackground" align="center" width="30%">Name</td>
                        <td class="DarkBackground" align="center" width="70%">Paremeter List</td>
                        -->
                        <th style="background: #2581C7; color: #FFFFFF; font-weight: bold;" align="center" width="30%">Name</td>
                        <th style="background: #2581C7; color: #FFFFFF; font-weight: bold;" align="center" width="70%">Paremeter List</td>
                    </tr>
                    <tbody id="operationsTableBody">
                    </tbody>
                </table>
    		</div> <!-- Operations tab -->

            <!-- Info tab -->
    		<div id="infoTab" dojoType="ContentPane" title="MBean Info" label="Info">
    		    <br>
                <table width="100%">
                    <tr>
                        <!--
                        <td class="DarkBackground" align="center" width="30%">Name</td>
                        <td class="DarkBackground" align="center" width="70%">Value</td>
                        -->
                        <th style="background: #2581C7; color: #FFFFFF; font-weight: bold;" align="center" width="30%">Name</td>
                        <th style="background: #2581C7; color: #FFFFFF; font-weight: bold;" align="center" width="70%">Value</td>
                    </tr>
                    <tbody id="basicInfoTableBody">
                    </tbody>
                </table>
            </div> <!-- Info tab -->

            <!-- Search tab -->
            <div id="searchTab" dojoType="ContentPane" title="Search" label="Search">
                <!-- JMXSearch Form -->
                <form name="JMXSearchForm" onsubmit="return false;">
                    <br>
            	    <table>
            	        <tr>
            	            <td width="15%">&nbsp;Object&nbsp;Name&nbsp;Pattern:</td>
            	            <td width="70%">
                                <select dojoType="combobox" id="jmxQuery" searchType="SUBSTRING" style="width: 100%;">
                                    <!-- Domains -->
                                    <option>*:*</option>
                                    <option>geronimo:*</option>
                                    <option>geronimo.config:*</option>
                                    <!-- J2EE MBeans -->
                                    <option>*:j2eeType=AppClientModule,*</option>
                                    <option>*:j2eeType=EJBModule,*</option>
                                    <option>*:j2eeType=EntityBean,*</option>
                                    <option>*:j2eeType=J2EEApplication,*</option>
                                    <option>*:j2eeType=J2EEDomain,*</option>
                                    <option>*:j2eeType=J2EEServer,*</option>
                                    <option>*:j2eeType=JavaMailResource,*</option>
                                    <option>*:j2eeType=JCAConnectionFactory,*</option>
                                    <option>*:j2eeType=JCAManagedConnectionFactory,*</option>
                                    <option>*:j2eeType=JCAResource,*</option>
                                    <option>*:j2eeType=JDBCDataSource,*</option>
                                    <option>*:j2eeType=JDBCDriver,*</option>
                                    <option>*:j2eeType=JDBCResource,*</option>
                                    <option>*:j2eeType=JMSResource,*</option>
                                    <option>*:j2eeType=JNDIResource,*</option>
                                    <option>*:j2eeType=JTAResource,*</option>
                                    <option>*:j2eeType=JVM,*</option>
                                    <option>*:j2eeType=MessageDrivenBean,*</option>
                                    <option>*:j2eeType=ResourceAdapter,*</option>
                                    <option>*:j2eeType=ResourceAdapterModule,*</option>
                                    <option>*:j2eeType=RMI_IIOPResource,*</option>
                                    <option>*:j2eeType=Servlet,*</option>
                                    <option>*:j2eeType=StatefulSessionBean,*</option>
                                    <option>*:j2eeType=StatelessSessionBean,*</option>
                                    <option>*:j2eeType=URLResource,*</option>
                                    <option>*:j2eeType=WebModule,*</option>
                                    <!-- Geronimo MBeans -->
                                    <option>*:j2eeType=AppClient,*</option>
                                    <option>*:j2eeType=ArtifactManager,*</option>
                                    <option>*:j2eeType=ArtifactResolver,*</option>
                                    <option>*:j2eeType=AttributeStore,*</option>
                                    <option>*:j2eeType=ConfigBuilder,*</option>
                                    <option>*:j2eeType=ConfigurationEntry,*</option>
                                    <option>*:j2eeType=ConfigurationManager,*</option>
                                    <option>*:j2eeType=ConfigurationStore,*</option>
                                    <option>*:j2eeType=CORBABean,*</option>
                                    <option>*:j2eeType=CORBACSS,*</option>
                                    <option>*:j2eeType=CORBATSS,*</option>
                                    <option>*:j2eeType=Deployer,*</option>
                                    <option>*:j2eeType=DeploymentConfigurer,*</option>
                                    <option>*:j2eeType=GBean,*</option>
                                    <option>*:j2eeType=Host,*</option>
                                    <option>*:j2eeType=JaasLoginService,*</option>
                                    <option>*:j2eeType=JACCManager,*</option>
                                    <option>*:j2eeType=JAXRConnectionFactory,*</option>
                                    <option>*:j2eeType=JCAActivationSpec,*</option>
                                    <option>*:j2eeType=JCAAdminObject,*</option>
                                    <option>*:j2eeType=JCAConnectionManager,*</option>
                                    <option>*:j2eeType=JCAConnectionTracker,*</option>
                                    <option>*:j2eeType=JCAResourceAdapter,*</option>
                                    <option>*:j2eeType=JCAWorkManager,*</option>
                                    <option>*:j2eeType=JMSConnector,*</option>
                                    <option>*:j2eeType=JMSPersistence,*</option>
                                    <option>*:j2eeType=JMSServer,*</option>
                                    <option>*:j2eeType=KeyGenerator,*</option>
                                    <option>*:j2eeType=Keystore,*</option>
                                    <option>*:j2eeType=LoginModule,*</option>
                                    <option>*:j2eeType=LoginModuleUse,*</option>
                                    <option>*:j2eeType=MEJB,*</option>
                                    <option>*:j2eeType=ModuleBuilder,*</option>
                                    <option>*:j2eeType=PersistentConfigurationList,*</option>
                                    <option>*:j2eeType=RealmBridge,*</option>
                                    <option>*:j2eeType=Repository,*</option>
                                    <option>*:j2eeType=RoleMapper,*</option>
                                    <option>*:j2eeType=SecurityRealm,*</option>
                                    <option>*:j2eeType=ServiceModule,*</option>
                                    <option>*:j2eeType=ServletTemplate,*</option>
                                    <option>*:j2eeType=ServletWebFilterMapping,*</option>
                                    <option>*:j2eeType=ServletWebServiceTemplate,*</option>
                                    <option>*:j2eeType=SystemLog,*</option>
                                    <option>*:j2eeType=TomcatValve,*</option>
                                    <option>*:j2eeType=TransactionContextManager,*</option>
                                    <option>*:j2eeType=TransactionLog,*</option>
                                    <option>*:j2eeType=TransactionManager,*</option>
                                    <option>*:j2eeType=URLPattern,*</option>
                                    <option>*:j2eeType=URLWebFilterMapping,*</option>
                                    <option>*:j2eeType=WebFilter,*</option>
                                    <option>*:j2eeType=WSLink,*</option>
                                    <option>*:j2eeType=XIDFactory,*</option>
                                    <option>*:j2eeType=XIDImporter,*</option>
                                    <option>*:j2eeType=XmlAttributeBuilder,*</option>
                                    <option>*:j2eeType=XmlReferenceBuilder,*</option>
                				</select>            	            
            	            </td>
            	            <td width="15%"><input type="button" value="Search" id="jmxSearch" onClick="searchBtnClicked()" style="width: 100%;" /></td>
            	        </tr>
            	    </table>
            	</form> <!-- JMXSearch Form -->
            </div> <!-- Search tab -->
            
        </div> <!-- Main tab container -->
        
	</div>  <!-- Horizontal split container -->
	
</div> <!-- Main layout container -->
