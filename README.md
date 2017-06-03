# InPlace Bundle Activator
Bundle Activator that manages and executes source bundles and plug-ins contained in an Eclipse development instance

For an overview see: http://javatime.no/blog/inplace-activator-overview/

To install see: http://marketplace.eclipse.org/content/inplace-bundle-activator

### Building

The build uses [Tycho](http://www.eclipse.org/tycho/).
 
To launch a complete build, select Run As | Maven Build ... from the parent pom file in project "no.javatime.inplace.parent" and issue

```
clean package
```
By default the build uses a Juno-based target platform. To use a different target (.e.g. neon) see the
platform-version-name attribute in the parent pom file in the "no.javatime.inplace.parent" project.

The corresponding target platform definitions can be found in `releng/no.javatime.inplace.targets`
