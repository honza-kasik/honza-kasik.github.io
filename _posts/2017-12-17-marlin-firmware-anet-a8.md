---
title: Marlin firmware on Anet A8 board
excerpt: Are you planning to upgrade your 3D printer to new firmware for any reason? Read my experience and instructions how to do it!
tags:
- Anet A8
- 3D print
---

Are you planning to upgrade your 3D printer to new firmware for any reason? Read my experience and instructions how to do it!

## Do I need it?

You probably already heard about SkyNet 3D - a Marlin firmware modification targeting Anet A6 and Anet A8. Recently, the Marlin firmware started supporting Anet A8, so there is no need for SkyNet anymore. Maybe you are asking what do you get by installing non original firmware. Here are advantages that I found:

* It's open source
* It's configurable
* It has auto level support
* Hot end protection is included

## Why not SkyNet 3D?

The main difference between SkyNet 3d and Marlin is how the configuration is done. For example there is a configuration option called `Z_SAFE_HOMING` which allows safe homing of z axis. This means that the z axis is homed after both x and y axis are done on the center of the bed. This becomes useful when you replace your z axis end switch by sensor. This options is enabled on SkyNet firmware by default, but disabled on Marlin firmware by default. Read along if you are interested what I mean by saying "disabled" and how do you configure these options. This looks like the only diference to me.

Based on this and that I am more into projects on GitHub than projects on Facebook and given that main SkyNet3D support is based on Facebook, everybody suggesting downloading SkyNet 3D firmware from some shady Google drives and latest SkyNet is based on four versions old Marlin – no, thanks, I will rather use Marlin.

## The source code

Source code? Why do I need that? It's simple. The whole "firmware update" thing is done through Arduino IDE. This application was originally made for supporting Arduino development boards, so users could create their own programs and load them to the hardware easily. Since Marlin firmware is distributed as a source code targeting this IDE, we will need to go through some of it to make basic configuration changes.

## Instructions

First some prerequisites. You will have to download following things:

* Arduino IDE – either [download it from website](https://www.arduino.cc/en/main/software#download) or if you use Linux, use your distribution's package manager. I did it on Fedora like this: `dnf install arduino`.
* [Anet A8 board support for Arduino IDE](https://github.com/SkyNet3D/anet-board) – you will need "drivers" for your Anet A8 board to upload new firmware to it. Just click on the green "Clone or Download" and choose "Download ZIP". Then follow attached instructions in README file. Just a note: On Linux, the Arduino folder is created in your home folder after first run of Arduino IDE.
* Marlin firmware release – visit [Marlin GitHub's releases page](https://github.com/MarlinFirmware/Marlin/releases) and download latest release (it was 1.1.7 in time of writing this article). It is under the link "Source code (zip)" or "Source code (tar.gz)" under the "Assets" header.

Now you should have installed Arduino IDE, "board drivers" and downloaded Marlin firmware.

### Arduino IDE configuration

1. Start Arduino IDE
1. If you have properly installed the Anet A8 board support, there should be now option "Anet A8 V1.0" in Board under Tools menu. Go ahead and select it. (do not select the optiboot variant)
1. If you haven't connected your printer to computer yet, you can connect it now.
1. After you connect the printer, there should be option available under Port in Tools menu. Select it. In my case, there was `/dev/ttyUSB0`.
1. Next thing is selecting programmer. Select "AVRISP mkII" under Programmer in Tools menu.
1. As a last thing, you have to install `LiquidCrystal.h` library to your Arduino IDE if you still use the original Anet A8 LCD. Just select Sketch -> Include library -> Manage libraries and in Library Manager window search "LiquidCrystal", then select latest version and click install button.

You are ready to use Arduino IDE for compiling and uploading new firmware!

### Firmware source code preparation

1. Unzip (extract) downloaded Marlin firmware to some location. Let's say, that it was extracted to folder named `Marlin-1.1.7`.
1. Open Arduino IDE again and select Open under File open. Navigate to `Marlin-1.1.7/Marlin` and open `Marlin.ino` file. This will open all source file from Marlin firmware in Arduino IDE.

### Firmware configuration

The main file in which all the configuration is made and the only file which you will ever have to edit is called `Configuration.h`. If some error occurs during it is either Marlin bug, missing package in Arduino IDE, or you set something bad in `Configuration.h`.

You won't be using default configuration file, because that would be too much editing to make. Instead, overwrite the default file by `Configuration.h` file from `Marlin-1.1.7/Marlin/example_configurations/Anet/A8` folder:

```
cd Marlin-1.1.7
cp Marlin/example_configurations/Anet/A8/Confiuguration.h Configuration.h
```

Now, if your printer is still connected and you performed everything in instructions, the code should successfully verify when you click on verify button in top left corner. If it verifies, click on upload button to upload new firmware to your Anet A8.

The configuration itself is dead simple. Just find an option you want to enable and remove comment operator `//`. If you want to disable that option, just comment it out. I suggest to enable `Z_SAFE_HOMING` option.

After each change you have to save the `Configuration.h` file and **reupload the firmware** so that the updated options are uploaded too.
