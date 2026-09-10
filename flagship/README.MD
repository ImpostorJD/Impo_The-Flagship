# RUNNNING SPRING BOOT COMMAND LINE BATCH
`./gradlew bootRun --args="<your arg>" `

./gradlew <taskName>
./gradlew bootJar && java -jar build/libs/flagship-0.0.1-SNAPSHOT.jar --app.runner=simple-chat-batch   

| Task | Needs stdin? | Use |
|---|---|---|
| `runFileRead` | No | `./gradlew runFileRead` |
| `runPdfFileRead` | No | `./gradlew runPdfFileRead -PfilePath=...` |
| `runPdfFileReadWithPdfbox` | No | `./gradlew runPdfFileReadWithPdfbox -PfilePath=...` |
| `runChat` | **Yes** | `./gradlew bootJar && java -jar build/libsflagship-0.0.1-SNAPSHOT.jar --app.runner=simple-chat-batch`|

NOTE: 
IF DONT WORK run as two separate task
./gradlew bootJar                                                                            
 java -jar build/libs/flagship-0.0.1-SNAPSHOT.jar --app.runner=simple-chat-batch