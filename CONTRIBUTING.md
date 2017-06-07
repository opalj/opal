# Contributing to OPAL
Everybody is welcome to contribute to OPAL and to submit pull requests. However, a pull request is only taken into consideration if you follow these guidelines. A recommended read (to speed up the process of getting your pull request pulled):
 [The Twitter Scala Style Guide](http://twitter.github.io/effectivescala/)

## Just One Commit
___The pull request consists of the **implementation of a single feature** and either consists of a single (squashed) commit or is done w.r.t. a specifically targeted feature branch___

If you have problems creating a pull-request that has only one commit, create the pull request w.r.t. some new (articial) branch. However, you always have to ensure that the pull request is minimal!

## Do not create pull requests w.r.t. the master branch
A pull request w.r.t. the master branch will directly be rejected. Integration with the master branch is always only done afer thourough testing by the main developer.

## Copyright Information
The copyright information (BSD License) was added to the file. This is an open-source project and we have to make sure that no one adds non open-source code. If you add other resources (icons etc.) to the project make sure that a license file is also found in the commit!

## Author Information
Author information was added where appropriate. It is important to be able to identify the author of some code to know where to get help/to know who is responsible.

## Tested
All existing unit and integration tests were successfully executed. Sufficient tests for the new code are included (use `Scalatest`. or `ScalaCheck` for the development and use `scoverage` for checking the coverage; the tests should check all features(!) and should have a coverage that is close to 100%.

## Formatted
The code is formatted using the same settings and style as the rest of the code (use the "Scalariform settings" as a basis); use the `sbt compileAll` command to ensure basic formatting!

## Documented
The code is reasonably documented.

## Code Conventions
Ensure that the code conventions w.r.t. naming and formatting are followed. 

Note, that some formatting conventions used by OPAL are not enforced by scalariform. In particular, **a line should not have more than 100 chars** (unless Scalariform always reformats the code such that the line has more than 100 chars which is, e.g., often the case for type declarations).

## Do Not Change Unrelated Code
Never reformat code that is not related to your task at hand; unless explicitly asked to do so. The commit should focus solely on your feature.
