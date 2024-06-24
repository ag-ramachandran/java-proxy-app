## Simple demo for proxy options in SDK


This is a simple demo to show how to use proxy options in Kusto java sdk. Till the 6.0.0 version of the SDK, these options are not there. Till this is released build this locally and use it in your project.

When 6.0.0+ is released this is an optional step!

```
git clone https://github.com/Azure/azure-kusto-java.git
mvn clean install -DskipTests
```

Once this is installed on the repo the main program can be run from VSCode. Just customize the .vscode/launch.json file to run the main program.

```json
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Current File",
            "request": "launch",
            "mainClass": "${file}"
        },
        {
            "type": "java",
            "name": "App",
            "request": "launch",
            "mainClass": "com.azure.kusto.App",
            // Cluster , tenant , database
            "args": ["https://<cluster>.kusto.windows.net", "<tenant>" , "<db>"],
            "projectName": "java-proxy-app"
        }
    ]
}
```

Note, I have not packaged this as a jar, so you need to run this from the IDE (just for technical brevity)
