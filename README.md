# 1MB Booster Events

Currently, a Helper for mcMMO Boost events for the 1MoreBlock.com Minecraft 1.16 Java server.

This is a plugin for Minecraft 1.16.5 (Spigot / Paper) made as a helper for mcMMO Overhaul 2.1.x. 

The purpose of this plugin is two-fold:

First, it will try to keep track of ongoing server (mcMMO /xprate) events, so when the server restarts it will try to automatically start it back up again. 

And secondly, players can at any time in-game type: `/rate` to find out if there is an event active, and if so, which rate the xp multiplier is set to.

More details about installation, configuration and usage can be found in the [wiki](https://github.com/mrfloris/mcmmoevent/wiki) pages.

## Origins

During the Minecraft 1.8 / 1.12.2 era with mcMMO Classic, I always wanted this feature. Someone from the Spigot community has helped me get started with this plugin. Unfortunately. I have lost the evidence of whom this was. It could have been KingTux, DefianceCoding, Nossr50 himself, my apologies. You're in the plugin.yml as Anonymous and are free to poke me for proper credits. 

This stopped working at some point in 1.13+, and the project was let go when we converted to mcMMO Overhaul.

## Where we are now

With Mojang's version 1.17 of Minecraft around the corner, I wanted to pick things up again for some projects. Including this one. I've updated it slightly, so it works _okay_ with 1.16.5, and mcMMO Overhaul. This now works on 64bit java8, java11 and java16. The next step is making it a bit more modern, follow the logic of its purpose a bit more. And prepping it for future features that I want to consider.

## Bugs / Suggestions

If you have an issue with this plugin, please make sure your Spigot or Paper engine is up to date, that you are on the correct version of mcMMO and are using the latest build of this 1MB Boosters plugin. 

When you're sure you've done everything right, you're free to [open an issue](https://github.com/mrfloris/mcmmoevent/issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBUG%5D) and file a bug report. We do not guarantee a fix, but we will do our best.

If you have a suggestion or feature request, feel free to [open a new discussion](https://github.com/mrfloris/mcmmoevent/discussions/new), and describe what you wish this plugin would include. We can at least read it and take it under consideration. 

## Other contributions

An honorable mention: Thank you [nossr50](https://github.com/nossr50), for making [mcMMO](https://github.com/mcMMO-Dev/mcMMO) in the first place. (And mcMMO is a reference to the plugin, it's not pretending to be mcMMO or a clone of it. No mcMMO code is included in this source.)

A logic issue showed up at 4 am, and thankfully so did [xsmeths](https://github.com/xsmeths/), he pointed out it was the if/else statements used for determining console output being flawed, it was showing twice due to it printing if the rate was not 1 and in an else after a check for if the rate was 1.

Thank you for the help buddy. < edit from xsmeths; you're welcome, happy to help you floris :-D > <Hugs>

Further contributions from [The456gamer](https://github.com/the456gamer) (thank you so much!)

And some suggestions from [zrips](https://github.com/zrips/) to help with improving efficiency.

## Version

[Tested build](https://github.com/mrfloris/mcmmoevent/releases) Version 1.0.27, for Spigot / Paper 1.16.5. Last updated: May 2021.
