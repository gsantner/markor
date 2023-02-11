# How I Take Notes With Vim, Markdown, and Pandoc
_15. May 2018_

**Notice: James Vaughan is the author of this article. It was first published on his [blog](https://jamesbvaughan.com/markdown-pandoc-notes/) in May 2018. 'I' refers to the original author.**

----------

I've gone through quite a few revisions of my note-taking process
and now that I'm in my final year of school,
I think I've finally found a system that I'm going to stick with.
In this post, I will describe this system and explain why I like it so much.

_**tl;dr**: I write notes in Markdown with Vim and Vimwiki on my computer
and with Markor on my phone,
keep them in sync with Syncthing,
and view them as web pages and PDFs that I generate with pandoc._

You might be wondering what things I'm taking notes on that are important
enough to warrant a whole post on how I take them.
Right now, the main thing is lectures for classes,
but I also take notes on:

- books that I read
- movies that I watch
- important conversations
- interesting things that I learn about people
- projects that I am work on
- recipes
- ideas for future blog posts

<!-- more -->

### My Note-Taking History

For some context,
in addition to taking notes with the method I describe in this post,
I have tried taking notes
with pen and paper,
with Google Docs,
with Evernote,
and
with Simplenote.

Of these methods, I stuck with pen and paper the longest.
I still like taking handwritten notes, but for most use cases
I value the ability to search through and format my notes
on my computer over the extra expressiveness of handwritten notes.

Evernote and Simplenote are great tools and I would recommend them for most
people, but I personally **prefer to keep my notes in simple files
on my filesystem that I can organize, modify, and parse however I want to.**

## How I Settled On This System

One problem that I've had with digital note-taking tools and apps is that
they're not Vim.
This might sound like a joke, but once you start using
Vim regularly, you can begin to feel handicapped without it.

_(For those who don't know what Vim is,
it's a text editor that encourages a completely mouse-free workflow.
It has an
extensive and elegant system of composable keybindings that enable users to
perform complex editing tasks with minimal hand movement.)_

Vim is not a [WYSIWYG](https://en.wikipedia.org/wiki/WYSIWYG)
editor, but I like using nice formatting features,
like
headings,
lists,
tables,
and pretty math,
so I needed to pick a markup language.
At first I thought I might use
[LaTeX](https://www.latex-project.org//)
because I was familiar with it and
it has nice default styles,
but ended up going with Markdown after discovering
[pandoc](http://pandoc.org/),
and learning that I could write documents in simple Markdown and
then use it to convert them
to LaTeX-formatted PDFs, HTML pages, and a bunch of other formats.

## Organization

I keep most of my notes in `~/Documents/notes`,
under subdirectories for different topics or types of notes.
For example, the notes for my computer security class are in
`~/Documents/notes/school/cs136`.
Within individual notes, I link to others with standard Markdown link syntax,
and can quickly navigate to them by placing my cursor over the link I want
to navigate to and pressing Enter, thanks to the
[Vimwiki](http://vimwiki.github.io/) plugin.

## Viewing

When I need to read my notes,
whether it's just for a quick reference
or to study for a big exam,
I have a few different methods set up.
<br/><br/>
#### In Vim

Vimwiki makes it really easy to navigate through a bunch of Markdown notes.
I have an `index.md` that looks something like this:

```markdown
---
title: My Knowledge Base
subtitle: >
  This is a collection of things that I know,
  things that I learn,
  and things that I want to remember.
---

## School

- [Computer Science](school/computer-science)
- [Math](school/math)
- [Physics](school/physics)

## Technologies

Tips and tricks on different applications and technologies that
I've found myself needing to look up more than once.

#### Languages

- [Go](technologies/go)
- [Python](technologies/python)

#### Tools

- [Postgres](technologies/postgres)
- [MySQL](technologies/mysql)
- [SSH](technologies/open-ssh)
- [Git](technologies/git)

#### Other

- [Linux Audio](technologies/linux-audio)
- [Progressive Web Apps](technologies/pwas)
- [Machine Learning](technologies/machine-learning)

## Misc

- [Favorite Film Moments](film-moments)
- [Recipes](recipes/index)
- [Project Ideas](projects/ideas)
- [Blog Post Ideas](blog-post-ideas)
- [Music to Listen To](music-to-listen-to)
- [Book Notes](books/index)
- [People](people/index)
- [Quotes I Like](quotes)
```

This file links to all my different categories of notes and
is a nice "home base" for them.
It also makes for a nice homepage when I convert the notes to a static
website.
<br/><br/>
#### On The Web

For quick things, the most common way I look at notes is actually with a web
browser.
I have a Makefile that converts all of my Markdown notes to HTML using
pandoc and deploys them to my server where they're served behind HTTP auth.
The Makefile looks something like this:

```makefile
MD_FILES=$(shell find . -name \*.md)
HTML_FILES=$(MD_FILES:.md=.html)
BUILD_HTML_FILES=$(HTML_FILES:%=build/%)

all: $(BUILD_HTML_FILES)

build/assets/%: assets/%
        @mkdir -p $$(dirname $@)
        cp $? $@

build/%.html: %.md template.html
  @mkdir -p $$(dirname $@)
  pandoc -o $@ --template=template.html $<

deploy:
        rsync --recursive --human-readable --delete --info=progress2 \
               build/* my_server
```

Right now I'm running this manually after I make changes to my notes that I
want to deploy but I might automate it in the future.
<br/><br/>
#### On My Phone

I use
[Syncthing](https://syncthing.net/)
to keep my notes directory in sync between my machines and my phone.
I also use the
[Markor](https://github.com/gsantner/markor)
app to manage and edit the notes on my phone.
This app is nice because it makes it easy to navigate my notes
directory and the built in editor formats Markdown files nicely.
<br/><br/>
### In Print

When I have a big exam coming up,
it sometimes helps to make a PDF of all my notes to study off of.
For this, I've created Makefiles for specific classes that produce a nice
looking PDF of all my notes for the class.
For example, this is my Makefile for a software engineering class I'm taking
right now:

```makefile
MD_FILES=about.md 130-final-notes.md general-advice.md requirements.md \
         software-processes.md modeling.md architectural-design.md \
         design-of-components.md software-quality.md \
         configuration-management.md testing.md week-2-discussion.md \
         week-3-discussion.md week-4-discussion.md
PDF_FILES=$(MD_FILES:.md=.pdf)
BUILD_PDF_FILES=$(PDF_FILES:%=build/%)
EXTRA_PDFS=sample-midterm-solutions.pdf

130.pdf: $(BUILD_PDF_FILES)
        gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/default \
                -dNOPAUSE -dQUIET -dBATCH -dDetectDuplicateImages
                -dCompressFonts=true -r150 -sOutputFile=$@ $^ $(EXTRA_PDFS)

build/%.pdf: %.md
        @mkdir -p $$(dirname $@)
        pandoc -V geometry:margin=1in -o $@ $?
```

This converts each file to a PDF and then uses
[Ghostscript](https://www.ghostscript.com/)
to combine them all into one.
It also lets me include any other PDFs I have, like the sample midterm in
my example.
This has been super handy for open-note tests.

## Caveats

I like this system a lot but it's not perfect.

One issue with it is that I have a lot of fun tweaking and "optimizing"
my Vim configuration and note taking process.
I put "optimizing" in quotes because this often ends up taking more of my
time than the "optimizations" actually save,
and I'll commonly miss chunks of lectures because I got distracted trying
to fix the syntax highlighting for misspelled words in my Vim colorscheme
or trying to decide on a better font size for the headers in my generated
PDFs.

Another area with room for improvement is in my use of the Vimwiki plugin.
It's a powerful plugin with a bunch of cool features, but
the only one I'm really using right now is the ability to navigate to linked
documents.
I think that my process could be improved by either using a more minimal
plugin that includes only that feature or by starting to take advantage of
more of Vimwiki's features.
