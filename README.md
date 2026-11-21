This is my terminal based E-Mail client.

It uses a .lua config, which will automatically be generated at "~/.config/termail/config.lua"
In the config.lua you HAVE to specify at least one E-Mail and its password, to use termail. For each E-Mail entry you can specify the starting directory, when reading Mails from that E-Mail.

For each E-Mail provider you end up using you NEED to enter the addresses and ports for smtp and imap. Now the programm should work.

As for visuals termail is supposed to the defaults you set for your terminal(font, fornt color, font weight, usw.).
You can currently customize how many linebreaks in a row you allow and if you want your text wrapped or not.

I added a spamfilter to the app itself, because it seems easier to configure one spam filter, wich will work for all E-Mails no matter the provider, than having to do it for like 5 seperate accounts.
You can automatically flag specific E-Mail-Addresses and specific domains as spam. Furthermore you can specify regex for the subject line f.e. "(i?)Black Week", so every mail with Black Week in the subject line will be flagged as Spam.
Now you can decide what to do with Spam, as default it will just be hidden from you, but you can also automatically delete or move it to different directory.

Install you can take a .jar from the releases and do whatever you want, or use the install.sh
The install.sh downloads the latest .jar from releases, adds "termail" as a starter keyword to your path and creates a launcher-entry(termail.desktop)

command stuff:

    "termail": this should just start the app in you terminal, if you used the install script
    arguments:
        "help": explainations and example commands
        "noui": runs the programm without ui
        "send": keyword to tell the programm you want to send an E-Mail
            "sender"='': the E-Mail you want the E-Mail to come from
            "reciever=''": the recipients E-Mail
            "subject=''": the subject line for the E-Mail
            "message=''": the message body
            "attachments={'example.pdf', 'example2.pdf'}": file paths to files you wish to send with this E-Mail 
