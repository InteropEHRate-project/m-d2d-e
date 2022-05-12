# InteropEHRate Device-to-Device (D2D) Protocol's HR Exchange Mobile Library

## Installation Guide
The process of integrating the `m-d2d-e` library is quite straightforward, as it is provided as a `jar` file, and is hosted in the project's Nexus repository. 

In case a gradle project is created, the following line needs to be inserted in the dependencies section of the build.gradle file:
```
implementation(group:'eu.interoperhate', name:'md2de', version: '0.3.5')
```

If the development team importing the library, is using Maven instead of Gradle, the same dependency must be expressed with the following Maven syntax:
```
<dependency>
	<groupId>eu.interopehrate</groupId>
	<artifactId>md2de</artifactId>
	<version>0.3.5</version>
</dependency>
```
