This is my terminal based E-Mail Client.

It uses a .lua config to connect to E-Mail-Servers it supports the use of multiple E-Mails at the same time and has a local Spam filter.

E-Mails can be sent with a one-line-command, the UI can be disabled (noui) in the run command to run some automations.
example command for an E-Mail:
    java -jar termail-1.0.0.jar noui send sender="example@email.com" reciever="example@email.com" subject="This is an example" message="This is the message" attachments={"path/to/your/file", "path/to/other/file"}

An overview of commands will be given through "help".

Current roadmap:
1. get an apprenticeship and an internship
2. code cleanup
3. Sending mails through TUI
4. Displaying a List for Attachments on E-Mails and reworking controls
   --probably stopping here or even earlier due to live--
6. Adding support to build TUI Elements to resemble Content-blocks in HTML-Mails
7. Trying my luck on a parser for .dot Files to HTML-Mails
