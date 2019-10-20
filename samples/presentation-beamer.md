---
class: beamer
---

-----------------
# Cool presentation

## Abed Nadir

{{ post.date_today }}

<!-- Overall slide design -->
<style>
.slide {
background:url() no-repeat center center fixed; background-size: cover;
}
.slide_type_title {
background: slategrey;
}
</style>

-----------------

# Slide title


1. All Markdown features of Markor are **supported** for Slides too ~~strikeout~~ _italic_ `code`
2. Start new slides with 3 more hyphens (---) separated by empty lines
3. End last slide with hyphens too
4. Slide backgrounds can be configured using CSS, for all and individual slides
5. Print / PDF export in landscape mode
6. Images can be centered by adding "imghcenter" in alt text, grow to page size with "imgbig"
7. Example: `![desc imghcenter imgbig](a.jpg)`


-----------------
# Slide with centered image
* imghcenter in description centers image, imgbig makes it big

![imghcenter imgbig](file:///android_asset/img/flowerfield.jpg)




-----------------
# Page with gradient background
* and a picture
* configure background color/image with CSS .slide_p4 { } (4 = the slide page number)

![pic](file:///android_asset/img/flowerfield.jpg)


<style> .slide_p4 { background: linear-gradient(to bottom, #11998e, #38ef7d); } </style>

-----------------
# Page with image background
* and text on top

| Left aligned | Middle aligned | Right aligned |
| :------------------- | :----------------------: | --------------------: |
| Test               | Test                    | Test                |
| Test               | Test                    | Test                |



<style> .slide_p5 { background: url('file:///android_asset/img/schindelpattern.jpg') no-repeat center center fixed; background-size: cover; } </style>

-----------------
