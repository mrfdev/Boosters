# mcMMO Events

Helper for mcMMO Boost events

This is a plugin for Minecraft 1.16.5 (Spigot / Paper) made as a helper for mcMMO Overhaul 2.1.x. 

The purpose of this plugin is two-fold:

First, it will try to keep track of ongoing mcMMO /xprate events, so when the server restarts it will try to automatically start it back up again. 

And secondly, players can at any time in-game type: `/rate` to find out if there is an event active, and if so, which rate the xp multiplier is set to.

## Origins

During the Minecraft 1.8 / 1.12.2 era with mcMMO Classic, I always wanted this feature. Someone from the Spigot community has helped me get started with this plugin. Unfortunately. I have lost the evidence of whom this was. It could have been KingTux, DefianceCoding, Nossr50 himself, my apologies. You're in the plugin.yml as Anonymous and are free to poke me for proper credits. 

This stopped working at some point in 1.13+, and the project was let go when we converted to mcMMO Overhaul.

## Where we are now

But, I picked things back up again, updated it slightly, so it works okay with 1.16.5, and mcMMO Overhaul. This now works on 64bit java8 and java16.

## Other contributions

An honorable mention: Thank you [nossr50](https://github.com/nossr50), for making [mcMMO](https://github.com/mcMMO-Dev/mcMMO) in the first place. (And mcMMO is a reference to the plugin, it's not pretending to be mcMMO or a clone of it. No mcMMO code is included in this source.)

A logic issue showed up at 4 am, and thankfully so did [xsmeths](https://github.com/xsmeths/), he pointed out it was the if/else statements used for determining console output being flawed, it was showing twice due to it printing if the rate was not 1 and in an else after a check for if the rate was 1.

Thank you for the help buddy. < edit from xsmeths; you're welcome, happy to help you floris :-D > <Hugs>

Further contributions from [The456gamer](https://github.com/the456gamer) (thank you so much!)

And some suggestions from [zrips](https://github.com/zrips/) to help with improving efficiency.

## Version

[Tested build](https://github.com/mrfloris/mcmmoevent/releases) Version 1.0.23, for Spigot / Paper 1.16.5. Last updated: May 2021.
