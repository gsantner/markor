---------------

# Markdown Reference
Automatically generate _table of contents_ by checking the option here: `Settings > Format > Markdown`.

## H2 Header
### H3 header
#### H4 Header
##### H5 Header
###### H6 Header

---------------

## Text

*Italic emphasis*
_Alternative italic emphasis_

**Bold emphasis**
__Alternative bold emphasis__

~~Strikethrough~~

Break line (two spaces at end of line)  

> Block quote

`Inline code`

```
Code blocks
are
awesome
```

---------------
 
## Lists
### Ordered & unordered

* Unordered list
* ...with asterisk/star
* Test

- Another unordered list
- ...with hyphen/minus
- Test

1. Ordered list
2. Test
3. Test
4. Test

- Nested lists
    * Unordered nested list
    * Test
    * Test
    * Test
- Ordered nested list
    1. Test
    2. Test
    3. Test
    4. Test
- Double-nested unordered list
    - Test
    - Unordered
        - Test a
        - Test b
    - Ordered
        1. Test 1
        2. Test 2

### Checklist
* [ ] Salad
* [x] Potatoes

1. [x] Clean
2. [ ] Cook

---------------

## Links
[Link](https://duckduckgo.com/)

[File in same folder as the document.](markor-markdown-reference.md) Use %20 for spaces!

---------------

## Tables

| Left aligned | Middle aligned | Right aligned |
| :--------------- | :------------------: | -----------------: |
| Test                 | Test                      | Test                    |
| Test                 | Test                      | Test                    |

÷÷÷÷

Shorter | Table | Syntax
:---: | ---: | :---
Test | Test | Test
Test | Test | Test

<!-- Comment: Not visibile in view. Can also be across multiple lines. -->

---------------

## Math (KaTeX)
See [reference](https://katex.org/docs/supported.html) & [examples](https://github.com/waylonflinn/markdown-it-katex/blob/master/README.md). Enable by checking Math at `Settings > Format > Markdown`.

### Math inline

$ I = \frac V R $

### Math block

<div>
$$\begin{array}{c} \nabla \times \vec{\mathbf{B}} -\, \frac1c\, \frac{\partial\vec{\mathbf{E}}}{\partial t} & = \frac{4\pi}{c}\vec{\mathbf{j}} \nabla \cdot \vec{\mathbf{E}} & = 4 \pi \rho \\ \nabla \times \vec{\mathbf{E}}\, +\, \frac1c\, \frac{\partial\vec{\mathbf{B}}}{\partial t} & = \vec{\mathbf{0}} \\ \nabla \cdot \vec{\mathbf{B}} & = 0 \end{array}$$
</div>

---------------

## Text (advanced)

### Text color

<span style='background-color:#ffcb2e;'>Text with background color / highlight</span>

<span style='color:#3333ff;'>Text foreground color</span>

<span style='text-shadow: 0px 0px 2px #FF0000;'>Text with colored outline</span> / <span style='text-shadow: 0px 0px 2px #0000FF; color: white'>Text with colored outline</span>


### Text sub & superscript

<u>Underline</u>

The <sub>Subway</sub> sandwich was <sup>super</sup>

Super special characters: ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ ™ ® ℠

### Text positioning
<div align='right'>

text on the **right**

</div>

<div align='center'>

text in the **center**  
(one empy line above and below  
required for Markdown support)

</div>

### Block Text

<div style='text-align: justify; text-justify: inter-word;'>
lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. 
</div>

---------------

## Multimedia

### Image
![Image](https://gsantner.net/assets/blog/img/markor/markor-v1-7-showcase-3.jpg)

### YouTube
<iframe width='360' height='200' src='https://www.youtube.com/embed/RJREFH7Lmm8'> </iframe>

### Link to video
[Road in the wood](https://open.tube/videos/watch/8116312a-dbbd-43a3-9260-9ea6367c72fc) (Peertube)
<div><video controls><source src='https://peertube.mastodon.host/download/videos/8116312a-dbbd-43a3-9260-9ea6367c72fc-480.mp4' </source></video></div>

<!--  Local video file <div><video controls><source src='voice-parrot.mp4' </source></video></div> -->

### Audio / Music
Yellowcard - Lights up in the sky

<audio controls src='../Music/mp3/Yellowcard/[2007]%20Paper%20Walls/Yellowcard%20-%2005%20-%20Light%20Up%20the%20Sky.mp3'></audio>

---------------

This Markdown reference file was created for the [Markor](https://gsantner.net/project/markor?source=markdownref) project by [Gregor Santner](https://gsantner.net) and is licensed [Creative Commons Zero 1.0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) (public domain). File revision 1.

---------------


