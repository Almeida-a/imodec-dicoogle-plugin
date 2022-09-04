# Imodec Dicoogle Plugin set
Imodec (Image [Modern] Codecs) is a set of plugins
for the [Dicoogle](https://github.com/bioinformatics-ua/dicoogle/)
project providing the services of modern image compression codecs.


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
Possible values are: `jxl`, `avif`, `webp` and `keep`.


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

