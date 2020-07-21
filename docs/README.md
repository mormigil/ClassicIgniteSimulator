# Mage Rotation Comparisons

The optimal rotation for your mage group is very dependent on the number of mages you have, the presence of world buffs, and the number of PIs your group has access to. All of the generated data was done with a standard set of mages that all have 98% hit, 21% crit, and 750 spell power.

In order to accomodate how different rotations perform against each other I generated a large dataset with a large variety of conditions. I've now represented this dataset as two parallel coordinates graphs.

The first graph which can be found [here](https://mormigil.github.io/ClassicIgniteSimulator/percent_dmg_increase.html) compares all the conditions to the dps increase over a standard 3 frostbolt rotation. The second graph can be found [here](https://mormigil.github.io/ClassicIgniteSimulator/dps.html) compares all the conditions to the total dps of the given conditions.

Both graphs can be filtered by dragging a line along any of the axes. This way you can find the optimal rotations for your particular mage team's situation. 

## Rotations

I had to use all numeric values for the parallel coordinates graph so the rotations are numbered as the indexes of a list. 

- Rotation 0: Three Frostbolts for all non-PI mages
- Rotation 1: All mages cast 1 frostbolt then fireball
- Rotation 2: Mages cast fireball without letting the scorch ignite drop
- Rotation 3: Mages cast fire blast instead of frostbolt to clear the scorch ignite
- Rotation 4: Mages cast fire blast as soon as it is available and a 5 stack ignite exists on the target
- Rotation 5: Non-PI mages stagger their combustion instead of all using it at once
- Rotation 6: 1 Mage casts scorch continously while ignite has 5 stacks and at least 1.5 seconds left
- Rotation 7: If there are less than 4 PI mages have mages up to 4 total cast one fireball after the scorch ignite is dropped. This way a non-PI mage cannot get the 5th cast, but the ignite is buil faster.

## Nightfall

I wanted to view the results of hunters or some melee using a nightfall at the start of a fight when the ignite is being built. To simulate this when the nightfall column is true (equals 1) there is a 30% chance of an activation at the 5 second timestamp. This means most if not all the initial ignites will be created with nightfall active.


