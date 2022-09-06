# Imodec Dicoogle Plugin set
Imodec (Image [Modern] Codecs) is a set of plugins
for the [Dicoogle](https://github.com/bioinformatics-ua/dicoogle/)
project providing the services of modern image compression codecs.

## Table of contents
1. [Building from source](#building-from-source)
2. [How to use](#how-to-use)
   1. [Pre-requisites](#pre-requisites)
   2. [Plugging into dicoogle](#plugging-into-dicoogle)
   3. [Store-SCU operation](#store-scu-operation)
      1. [storescu - dcmtk](#storescu---dcmtk)
      2. [dicom-storescu - dicom-rs](#dicom-storescu---dicom-rs)
   4. [View the resulting images](#view-the-resulting-images)
      1. [Http Request Structure](#http-request-structure)
3. [Other Notes](#other-notes)
   1. [New transfer syntaxes](#new-transfer-syntaxes)
   2. [Contributing](#contributing)
   3. [Configuring encoding options](#configuring-encoding-options)

## Building from source
If you want, you can build from source using the `mvn`
building tool.

For that, just run:
```shell
mvn install
```

At the root directory of this project.

## How to use

### Pre-requisites

This plugin set can only be used in Linux.
Other OSs are not (yet) supported.

You need to install
[rust](https://www.rust-lang.org/tools/install):
```shell
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

You also need to install the following codecs
to your machine:
```shell
sudo apt install webp
cargo install cavif avif-decode
```

### Plugging into Dicoogle

In order to use this plugin, just copy the generated
*_jar-with-dependencies_ jar file to the Dicoogle
Plugins folder, like the following example:

```shell
cp ~/imodec-dicoogle-plugin/target/imodec-dicoogle-plugin-0.0.0-jar-with-dependencies.jar ~/DicoogleDir/Plugins
```

### Store-SCU operation

This is where the modern codec encoding takes place.
You can choose the default format to encode
the images, by adding the following `codec` tag to the
xml settings file (path `Plugins/settings/imodec-plugin-set.xml`):
```xml
<configuration>
    ...
    <codec>jxl</codec>
</configuration>
```
Possible values are: `jxl`, `avif`, `webp`, `keep` and `all` for all the previous options simultaneously.


You need to use a specific tool for the store operation.
One of the following tools are suggested.

#### storescu - dcmtk
To install:
```shell
sudo apt install dcmtk
```

To use (using the default aetitle and
port configurations for the dicoogle PACS):
```shell
storescu -aec DICOOGLE-STORAGE localhost 6666 ...FILES
```

#### dicom-storescu - dicom-rs

To install the tool:
```shell
cargo install dicom-storescu
```

To use:
```shell
dicom-storescu --called-ae-title DICOOGLE-STORAGE localhost:6666 ...FILES
```

### View the resulting images
In order to check the stored images, you need to
input an url to your browser with the
SOP Instance UID of the respective dicom object,
following the next example:
```http request
http://localhost:8080/imodec/view?siuid=2.25.69906150082773205181031737615574603347&codec=jxl
```

#### Http request structure
Base url:
```http request
http://localhost:8080/imodec/view
```

Parameters:
 * `siuid` [Required]: SOP Instance UID of the dicom object's image to be viewed.
 * `tsuid` [Optional]: Transfer Syntax UID defining a version of the dicom object in a specific format.
 * `codec` [Optional]: If you want to see the image of a specific modern format, choose here which format that 
you want to see. This is the same as choosing the [transfer syntax](#new-transfer-syntaxes) of that specific codec with the
above parameter. If both are used, `tsuid` overrides `codec`.


## Other Notes

### New Transfer Syntaxes

The new image formats will encode the pixel data of the dicom objects.
The transfer syntaxes define the format of the pixel-data of the dicom objects.
Thus, new transfer syntaxes are created to define pixel-data with the bitstream of those modern codecs.

New Transfer-Syntax list:
 * JPEG-XL: `1.2.826.0.1.3680043.2.682.104.1`
 * WebP: `1.2.826.0.1.3680043.2.682.104.2`
 * AVIF: `1.2.826.0.1.3680043.2.682.104.3`

### Contributing
This project encompasses developing a set of plugins for the dicoogle software. Therefore, for anyone interested in contributing, imodec follows the [dicoogle development guidelines](https://github.com/bioinformatics-ua/dicoogle/wiki#development-guidelines).

### Configuring encoding options

This is a more advanced configuration.
You can define encoding options such as quality or speed of compression
(depending on the name of the configuration parameters).

An example of those options is displayed below:
```xml
<configurations>
   ...
   <jxl distance="1.0" effort="7" />
   <avif quality="90" speed="4" />
   <webp quality="90" speed="4" />
</configurations>
```