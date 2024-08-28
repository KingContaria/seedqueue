# SeedQueue

## Description

SeedQueue is a mod for Minecraft Speedrunning that enables quick resetting of worlds by loading multiple worlds simultaneously. Designed to replace multi-instancing, SeedQueue can achieve this with only one instance of Minecraft.

### Key Features

- Easy to setup
- Built-in "Wall" screen
- Improved performance, particularly on lower-end hardware
- Highly customizable with in-game options and resource packs

## Installation

> [!IMPORTANT]
> SeedQueue is only compatible with Fabric Loader version 16.0 and later.

### Prerequisites

1. **Fabric Installation:**  
 Ensure [Fabric](https://fabricmc.net/use/installer/) is installed. [MultiMC](https://multimc.org/) and [Prism](https://prismlauncher.org/) are easy tools for installing Fabric.

1. **Compatible Mods:**  
 SeedQueue only works with mods approved for speedrunning. You can find the list of compatible mods on the [MCSR Mod List](https://mods.tildejustin.dev/).

### Installation Steps

1. **Download SeedQueue:**  
 [Download the latest release](https://github.com/KingContaria/seedqueue/releases/latest) of SeedQueue.

1. **Place in Mod Folder:**  
 Put the SeedQueue `.jar` file and its dependencies in your `mods` folder.

1. **Launch the Game:**  
 Happy resetting and good luck!

### Dependencies

All the mods listed below can be downloaded from the [MCSR Mod List](https://mods.tildejustin.dev/).
| Mod | Description | Required | Required for Wall Screen | Recommended |
| :----------------- | :----------------------------------------------------------------------------------------------------------------: | :----------------: | :----------------------: | :----------------: |
| Atum | Automatically reset RSG and SSG | ✅ | | ✅ |
| SpeedrunAPI | API for MCSR mods, providing a Config API, selected Fabric API modules, and MixinExtras. | ✅ | | ✅ |
| WorldPreview | Makes you reset block game fast | | ✅ | ✅ |
| Sodium | Sodium is a free and open-source optimization mod for Minecraft that improves frame rates and reduces lag spikes. | | ✅ | ✅ |
| FastReset | Skips saving worlds to reset faster. | | ✅ | ✅ |
| AntiResourceReload | Disables the monitored reload upon world creation. | | | ✅ |
| Starlight | Rewrites the light engine to fix lighting performance and lighting errors | | | ✅ |

## Getting Started

If you're new to SeedQueue, watch the video below to get started

https://github.com/user-attachments/assets/bd303011-90c1-4ab5-ad05-5ce4e3de983e

## Documentation

For more information on configuration settings and customization options for the Wall Screen, visit the [SeedQueue Wiki](https://github.com/KingContaria/seedqueue/wiki).

## Community

Have questions, need help, or just want to hang out in the community? Join the SeedQueue community on Discord by clicking [here](https://discord.gg/9P6PJkHCdU).

## FAQs

### Can I use SeedQueue with other mods?

SpeedQueue is only compatible with mods approved for speedrunning. You can find the list of compatible mods on the [MCSR Mod List](https://mods.tildejustin.dev/).

### How do I exit the Wall Screen?

Pressing `shift + esc` will end resetting and take you back to the main menu.

### How can I make my previews pause with F3 on the Wall?

Go to StandardSettings and enable F3 Pause on World Load.

### How do I change textures/sounds/wall layout?

You can customize using a resource,
I recommend downloading the example pack [here](https://github.com/KingContaria/julti-dynamic-resourcepack/releases/tag/1.0). You can find out everything you need to know in [the wiki](https://github.com/KingContaria/seedqueue/wiki/Customization). If you have any more questions about stuff that isn't working the way you want it to please create a post in the [⁠support forum](https://discord.gg/nq4zcusTDP).

### Why are no previews showing up in the 'preparing' group of my custom layout?

Go to `Performance Settings > Background Previews` and set it to the size of your preparing group.

### How can I limit reset sounds?

By default, SeedQueue will play one reset sound for every preview that gets reset.
You can add a Reset All sound that will play instead of a bunch of individual reset sounds with a resource pack.

### How can I show my reset counter on OBS?

1) Open OBS
2) Go to Sources
3) Click the `+` icon
4) Select Text
5) Name it and press Okay
6) Check the checkbox that says `Read from file`
7) Add `<pathtoyourinstance>/.minecraft/config/mcsr/atum/rsg-attempts.txt as file`
8) Press Okay to create the text source

### When will SeedQueue be ported to other versions?

SeedQueue relies on porting a bunch of other mods first, so it's going to take a while. Additionally, there will be at the very least a month between release and starting porting efforts to catch any issues or implement important feedback.
