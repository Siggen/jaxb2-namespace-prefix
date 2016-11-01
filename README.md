# jaxb2-namespace-prefix
Jaxb2 'namespace-prefix' plugin that adds `javax.xml.bind.annotation.XmlNs` annotations to `package-info.java` file according to 
specific definition in the bindings.xml file. Those annotations associate namespace prefixes with XML namespace URIs.


# Example

The following package-info.java is generated automatically with the XmlNs annotation :

```
@javax.xml.bind.annotation.XmlSchema(namespace = "http://www.ech.ch/xmlns/eCH-0007/3", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED, xmlns = {
    @javax.xml.bind.annotation.XmlNs(namespaceURI = "http://www.ech.ch/xmlns/eCH-0007/3", prefix = "eCH-0007")
})
package ch.ech.ech0007.v3;
```

And then, Jaxb2 will build Xml structure that look like this :

```
<?xml version="1.0" encoding="UTF-8"?>
<eCH-0007:municipalityRoot xmlns:eCH-0007="http://www.ech.ch/xmlns/eCH-0007/3">
    <eCH-0007:swissMunicipalityType>
        ...
    </eCH-0007:swissMunicipalityType>
</eCH-0007:municipalityRoot>
```

Instead of the default prefix numbering scheme :

```
<?xml version="1.0" encoding="UTF-8"?>
<ns1:municipalityRoot xmlns:ns1="http://www.ech.ch/xmlns/eCH-0007/3">
    <ns1:swissMunicipalityType>
        ...
    </ns1:swissMunicipalityType>
</ns1:municipalityRoot>
```

# Usage

Example's configuration with the maven-jaxb2-plugin :

```
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>0.8.0</version>
    <configuration>
        <schemaDirectory>src/main/resources</schemaDirectory>
        <catalog>src/main/resources/catalog.xml</catalog>
        <schemaIncludes>
            <include>*.xsd</include>
        </schemaIncludes>
        <bindingDirectory>src/main/resources</bindingDirectory>
        <bindingIncludes>
            <include>bindings.xml</include>
        </bindingIncludes>
        <args>
            <arg>-extension</arg>
            <arg>-Xnamespace-prefix</arg>
        </args>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>org.jvnet.jaxb2_commons</groupId>
            <artifactId>jaxb2-namespace-prefix</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```

Example of bindings.xml file :

```
<?xml version="1.0"?>
<jxb:bindings version="1.0"
    xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:namespace="http://jaxb2-commons.dev.java.net/namespace-prefix"
    xsi:schemaLocation="http://java.sun.com/xml/ns/jaxb http://java.sun.com/xml/ns/jaxb/bindingschema_2_0.xsd
    http://jaxb2-commons.dev.java.net/namespace-prefix https://raw.githubusercontent.com/Siggen/jaxb2-namespace-prefix/master/src/main/resources/prefix-namespace-schema.xsd">

    <jxb:bindings schemaLocation="eCH-0007-3-0.xsd">
        <jxb:schemaBindings>
            <jxb:package name="ch.ech.ech0007.v3" />
        </jxb:schemaBindings>
        <jxb:bindings>
            <namespace:prefix name="eCH-0007" />
        </jxb:bindings>
    </jxb:bindings>
</jxb:bindings>
```

# Release notes

 - Version 1.1 (2012.06.12) : Implemented support for multiple schemas (with different namespaces) that bind to the same java package.
 - Version 1.0 (2012.06.01) : First version.
