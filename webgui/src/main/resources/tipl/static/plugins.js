function showPlugins(plugNames,plugDesc) {
    $("#form").alpaca({
                    "schema": {
                        "title":"TIPL Tool Runner",
                        "description":"Select the plugin for further analysis",
                        "type":"object",
                        "properties": {
                            "pluginSelector": {
                                "type":"string",
                                "title":"Choose Plugin",
                                "enum":plugNames
                            },
                            "pluginName": {
                                "type":"string",
                                "title":"Name"
                            },
                            "pluginParameter": {
                                "type":"string",
                                "title":"Choose Parameter",
                                "enum":[]
                            }
                        }
                    },
                    "options": {
                        "form":{
                            "attributes":{
                                "action":"http://httpbin.org/post",
                                "method":"post"
                            },
                            "buttons":{
                                "submit":{}
                            }
                        },
                        "fields": {
                        "pluginSelector": {
                                "type": "select",
                                "helper": "Select your ranking.",
                                "optionLabels": plugDesc
                            }
                        }
                    },
                    "postRender": function(control) {
                        var plugin = control.childrenByPropertyId["pluginSelector"];
                        var pname = control.childrenByPropertyId["pluginParameter"];
                        console.log(plugin);
                        pname.subscribe(plugin, function(val) {
                            this.schema.enum = plugNames;
                            this.refresh();
                        });
                    }
                });
}