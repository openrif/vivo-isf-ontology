This is an Eclipse Java project for an initial set of simple scripts and classes to help with generating an ISF release. Each class might have additional comments but the main content is:

1. An "isf" package with a ISFUtil class. This class is meant to hold ISF wide constants and useful OWL-API static methods.
2. An "isf.ignore" package which contains a couple of old classes that did one time tasks. This package can be ignored.
3. An "isf.module" that contains a BuildModule class to build a specific named module based on a parameter, and a ModuleUtil class to support the builder.
4. An "isf.release" for release classes. Run the _DoRelease to create release files.
5. "isf.release.action" for release actions and reporting. When a release class is ran, it does some "action"s and uses a Reporter to generate a text file for the results of the action.
6. The "launcher" folder contains Eclipse Java launcher configurations. See the specific parameters that need to be set to run. Specifically, a variable has to be set that points to the top SVN directory of your local checkout (not trunk, but its parent).
7. The lib/lib-src folder has the required libraries, and their sources.


