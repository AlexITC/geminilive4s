# Examples

These are example applications that can be executed through [scala-cli](https://scala-cli.virtuslab.org/), like:

- When running from the repo: `scala-cli NoteTakerDemo.scala`
- When running from the file url: `scala-cli https://raw.githubusercontent.com/AlexITC/geminilive4s/refs/heads/main/examples/NoteTakerDemo.scala`


## Available examples:

- [MinimalDemo.scala](./MinimalDemo.scala): Simplest example to connect send the audio from your microphone to Gemini, Gemini's output is played through the speaker.
- [NoteTakerDemo.scala](./NoteTakerDemo.scala): Gemini ask for your day, preparing a summary which is shared to the code by invoking a custom function.
- [TakeManualTurns.scala](./TakeManualTurns.scala): Disable Voice Activity Detection, instead, the code tells Gemini when to start speaking.
