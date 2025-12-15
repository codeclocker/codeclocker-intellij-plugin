# TODO

## Bugs

### Skip counting gifs from change samples

When user commits gif deletion, `ChangesActivityTracker` counts every gif line as 'removal'. 
As a result user has too many deleted lines of code in his stats.
DB row example: [example](attach/img.png)

### Plugin continue sending samples after its uninstallation

STRs:
1. Uninstall plugin from IntelliJ.
2. Do not restart it.

ER:
1. Activity is stopped being reported.

AR:
1. Activity continue being reported (every sample has 60 seconds of spent time).

Hub logs:

```
2025-11-16 09:12:05.034 [] [nio-8080-exec-9] DEBUG c.c.h.c.ActivitySampleController         : Saving time spent sample [TimeSpentSampleByProjectDto(projects={codeclocker-intellij-plugin=TimeSpentSampleDto(samplingStartedAt=1763284262324, timeSpentSeco
nds=60)})] for API key [cc-505d5d0c-1545-4f1f-a0d3-484013c2c6d4]
```
