# Repository rules

This file contains rules for the repository

## Pushing changes

Before you start working on new features, bug fixes, or any other change, **create a new branch for that change**<br>
You can do this with the git command ``git branch <branch-name>``, then checking out to that branch with ``git checkout <branch-name>``

Branch naming convention examples:<br>
- ``bugfix/somechange`` | Branch name for bugfixes
- ``feature/newfeature`` | Branch name for new features. This applies to adding new functionality to an existing feature aswell
- ``docs/somedocumentation`` | Branch name for documentation changes and additions

This isn't a strict rule, you can come up with your own branch names but please still tag your branches with this convention.<br>
For example, if you're rewriting a feature, you may use a branch name ``rewrite/somefeature``

Changes cannot be force pushed into main. Only the following files can be force pushed:
- README.md
- PLAN.md
- "Team meeting and details log.md"
- requirements.md

## Formatting

You are expected to use the Flake8 linter. E501 can be ignored, and will be added to the projects .flake8 file later on.<br>
Formatting will be checked by gitea whenever a new change is pushed. If it fails, the error needs to be corrected before the change can be merged.