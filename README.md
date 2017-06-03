# InPlace Bundle Activator
Bundle Activator that manages and executes source bundles and plug-ins contained in an Eclipse development instance.

For an overview see: [InPlace Activator Overview](http://javatime.no/blog/inplace-activator-overview/).

### Installing

To install see: [Eclipse MarketPlace](http://marketplace.eclipse.org/content/inplace-bundle-activator/) 
or [InPlace Install Site](http://javatime.no/blog/download-2/).

### Building

The build uses [Tycho](http://www.eclipse.org/tycho/).
 
To launch a complete build, select: 
```
Run As | Maven build...; and Run the goal; clean package
```
from the parent pom file in project `packaging/no.javatime.inplace.parent`.

After a build the local update site can be found in: `packaging/no.javatime.inplace.parent/target/repository`.

### Targets

By default the build uses a Juno-based target platform. To use a different target (e.g. neon) see the
platform-version-name attribute in the parent pom file in the `packaging/no.javatime.inplace.parent` project.

The corresponding target platform definitions can be found in the `releng/no.javatime.inplace.targets` project.
