/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.format.zimwiki.ZimWikiTextActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.AndroidSpinnerOnItemSelectedAdapter;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.io.File;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;
import other.de.stanetz.jpencconverter.PasswordStore;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "net.gsantner.markor.ui.NewFileDialog";
    public static final String EXTRA_DIR = "EXTRA_DIR";
    public static final String EXTRA_ALLOW_CREATE_DIR = "EXTRA_ALLOW_CREATE_DIR";
    private Callback.a2<Boolean, File> callback;

    public static NewFileDialog newInstance(final File sourceFile, final boolean allowCreateDir, final Callback.a2<Boolean, File> callback) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        args.putSerializable(EXTRA_ALLOW_CREATE_DIR, allowCreateDir);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);
        final boolean allowCreateDir = getArguments().getBoolean(EXTRA_ALLOW_CREATE_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, allowCreateDir, inflater);
        AlertDialog dialog = dialogBuilder.show();
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    @SuppressLint("SetTextI18n")
    private AlertDialog.Builder makeDialog(final File basedir, final boolean allowCreateDir, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        final AppSettings appSettings = new AppSettings(inflater.getContext());
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), appSettings.isDarkThemeEnabled() ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final CheckBox encryptCheckbox = root.findViewById(R.id.new_file_dialog__encrypt);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template);
        final String[] typeSpinnerToExtension = getResources().getStringArray(R.array.new_file_types__file_extension);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && appSettings.hasPasswordBeenSetOnce()) {
            encryptCheckbox.setChecked(appSettings.getNewFileDialogLastUsedEncryption());
        } else {
            encryptCheckbox.setVisibility(View.GONE);
        }
        fileExtEdit.setText(appSettings.getNewFileDialogLastUsedExtension());
        fileNameEdit.requestFocus();
        new Handler().postDelayed(new ContextUtils.DoTouchView(fileNameEdit), 200);

        fileNameEdit.setFilters(new InputFilter[]{ContextUtils.INPUTFILTER_FILENAME});
        fileExtEdit.setFilters(fileNameEdit.getFilters());

        final AtomicBoolean typeSpinnerNoTriggerOnFirst = new AtomicBoolean(true);
        typeSpinner.setOnItemSelectedListener(new AndroidSpinnerOnItemSelectedAdapter(pos -> {
            if (pos == 3) { // Zim
                templateSpinner.setSelection(7); // Zim empty
            }
            if (typeSpinnerNoTriggerOnFirst.getAndSet(false)) {
                return;
            }
            String ext = pos < typeSpinnerToExtension.length ? typeSpinnerToExtension[pos] : "";

            if (ext != null) {
                if (encryptCheckbox.isChecked()) {
                    fileExtEdit.setText(ext + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                } else {
                    fileExtEdit.setText(ext);
                }
            }
            fileNameEdit.setSelection(fileNameEdit.length());
            appSettings.setNewFileDialogLastUsedType(typeSpinner.getSelectedItemPosition());
        }));
        typeSpinner.setSelection(appSettings.getNewFileDialogLastUsedType());

        templateSpinner.setOnItemSelectedListener(new AndroidSpinnerOnItemSelectedAdapter(pos -> {
            String prefix = null;

            if (pos == 3) { // Jekyll
                prefix = TodoTxtTask.DATEF_YYYY_MM_DD.format(new Date()) + "-";
            } else if (pos == 9) { //ZettelKasten
                prefix = new SimpleDateFormat("yyyyMMddHHmm", Locale.ROOT).format(new Date()) + "-";
            }
            if (!TextUtils.isEmpty(prefix) && !fileNameEdit.getText().toString().startsWith(prefix)) {
                fileNameEdit.setText(prefix + fileNameEdit.getText().toString());
            }
            fileNameEdit.setSelection(fileNameEdit.length());
        }));

        encryptCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final String currentExtention = fileExtEdit.getText().toString();
            if (isChecked) {
                if (!currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                    fileExtEdit.setText(currentExtention + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                }
            } else if (currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                fileExtEdit.setText(currentExtention.replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, ""));
            }
            appSettings.setNewFileDialogLastUsedEncryption(isChecked);
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();

        final ShareUtil shareUtil = new ShareUtil(getContext());
        dialogBuilder
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }

                    appSettings.setNewFileDialogLastUsedExtension(fileExtEdit.getText().toString().trim());
                    final String usedFilename = getFileNameWithoutExtension(fileNameEdit.getText().toString(), templateSpinner.getSelectedItemPosition());
                    final File f = new File(basedir, usedFilename.trim() + fileExtEdit.getText().toString().trim());
                    final byte[] templateContents = getTemplateContent(templateSpinner, basedir, f.getName(), encryptCheckbox.isChecked());
                    shareUtil.writeFile(f, false, (arg_ok, arg_fos) -> {
                        try {
                            if (f.exists() && f.length() < ShareUtil.MIN_OVERWRITE_LENGTH && templateContents != null) {
                                arg_fos.write(templateContents);
                            }
                        } catch (Exception ignored) {
                        }
                        callback(arg_ok || f.exists(), f);
                        dialogInterface.dismiss();
                    });
                })
                .setNeutralButton(R.string.folder, (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }
                    final String usedFoldername = getFileNameWithoutExtension(fileNameEdit.getText().toString(), templateSpinner.getSelectedItemPosition());
                    File f = new File(basedir, usedFoldername);
                    if (shareUtil.isUnderStorageAccessFolder(f)) {
                        DocumentFile dof = shareUtil.getDocumentFile(f, true);
                        callback(dof != null && dof.exists(), f);
                    } else {
                        callback(f.mkdirs() || f.exists(), f);
                    }
                    dialogInterface.dismiss();
                });

        if (!allowCreateDir) {
            dialogBuilder.setNeutralButton("", null);
        }

        return dialogBuilder;
    }

    private boolean ez(EditText et) {
        return et.getText().toString().isEmpty();
    }

    private String getFileNameWithoutExtension(String typedFilename, int selectedTemplatePos) {
        if (selectedTemplatePos == 7) {
            // zim wiki files always use underscores instead of spaces
            return typedFilename.trim().replace(' ', '_');
        }
        return typedFilename;
    }

    private void callback(boolean ok, File file) {
        try {
            callback.callback(ok, file);
        } catch (Exception ignored) {
        }
    }

    // How to get content out of a file:
    // 1) Replace \n with \\n | copy to clipboard
    //    cat markor-markdown-reference.md  | sed 's@\\@\\\\@g' | sed -z 's@\n@\\n@g'  | xclip
    //
    // 2) t = "<cursor>";  | ctrl+shift+v "paste without formatting"
    //
    @SuppressLint("TrulyRandom")
    private byte[] getTemplateContent(final Spinner templateSpinner, final File basedir, final String filename, final boolean encrypt) {
        String t = null;
        byte[] bytes = null;
        switch (templateSpinner.getSelectedItemPosition()) {
            case 1: {
                t = "# Markdown Reference\nAutomatically generate _table of contents_ by checking the option here: `Settings > Format > Markdown`.\n\n## H2 Header\n### H3 header\n#### H4 Header\n##### H5 Header\n###### H6 Header\n\n<!-- --------------- -->\n\n## Format Text\n\n*Italic emphasis* , _Alternative italic emphasis_\n\n**Bold emphasis** , __Alternative bold emphasis__\n\n~~Strikethrough~~\n\nBreak line (two spaces at end of line)  \n\n> Block quote\n\n`Inline code`\n\n```\nCode blocks\nare\nawesome\n```\n\n<!-- --------------- -->\n \n## Lists\n### Ordered & unordered\n\n* Unordered list\n* ...with asterisk/star\n* Test\n\n- Another unordered list\n- ...with hyphen/minus\n- Test\n\n1. Ordered list\n2. Test\n3. Test\n4. Test\n\n- Nested lists\n    * Unordered nested list\n    * Test\n    * Test\n    * Test\n- Ordered nested list\n    1. Test\n    2. Test\n    3. Test\n    4. Test\n- Double-nested unordered list\n    - Test\n    - Unordered\n        - Test a\n        - Test b\n    - Ordered\n        1. Test 1\n        2. Test 2\n\n### Checklist\n* [ ] Salad\n* [x] Potatoes\n\n1. [x] Clean\n2. [ ] Cook\n\n<!-- --------------- -->\n\n## Links\n[Link](https://duckduckgo.com/)\n\n[File in same folder as the document.](markor-markdown-reference.md) Use %20 for spaces!\n\n<!-- --------------- -->\n\n## Tables\n\n| Left aligned | Middle aligned | Right aligned |\n| :--------------- | :------------------: | -----------------: |\n| Test                 | Test                      | Test                    |\n| Test                 | Test                      | Test                    |\n\n÷÷÷÷\n\nShorter | Table | Syntax\n:---: | ---: | :---\nTest | Test | Test\nTest | Test | Test\n\n<!-- Comment: Not visibile in view. Can also span across multiple lines. End with:-->\n\n<!-- --------------- -->\n\n## Math (KaTeX)\nSee [reference](https://katex.org/docs/supported.html) & [examples](https://github.com/waylonflinn/markdown-it-katex/blob/master/README.md). Enable by checking Math at `Settings > Markdown`.\n\n### Math inline\n\n$ I = \\frac V R $\n\n### Math block\n\n<div>\n$$\\begin{array}{c} \\nabla \\times \\vec{\\mathbf{B}} -\\, \\frac1c\\, \\frac{\\partial\\vec{\\mathbf{E}}}{\\partial t} & = \\frac{4\\pi}{c}\\vec{\\mathbf{j}} \\nabla \\cdot \\vec{\\mathbf{E}} & = 4 \\pi \\rho \\\\ \\nabla \\times \\vec{\\mathbf{E}}\\, +\\, \\frac1c\\, \\frac{\\partial\\vec{\\mathbf{B}}}{\\partial t} & = \\vec{\\mathbf{0}} \\\\ \\nabla \\cdot \\vec{\\mathbf{B}} & = 0 \\end{array}$$\n</div>\n\n\n$$\\frac{k_t}{k_e} = \\sqrt{2}$$\n\n<!-- --------------- -->\n\n## Format Text (continued)\n\n### Text color\n\n<span style='background-color:#ffcb2e;'>Text with background color / highlight</span>\n\n<span style='color:#3333ff;'>Text foreground color</span>\n\n<span style='text-shadow: 0px 0px 2px #FF0000;'>Text with colored outline</span> / <span style='text-shadow: 0px 0px 2px #0000FF; color: white'>Text with colored outline</span>\n\n\n### Text sub & superscript\n\n<u>Underline</u>\n\nThe <sub>Subway</sub> sandwich was <sup>super</sup>\n\nSuper special characters: ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ ™ ® ℠\n\n### Text positioning\n<div markdown='1' align='right'>\n\ntext on the **right**\n\n</div>\n\n<div markdown='1' align='center'>\n\ntext in the **center**  \n(one empy line above and below  \nrequired for Markdown support OR markdown='1')\n\n</div>\n\n### Block Text\n\n<div markdown='1' style='text-align: justify; text-justify: inter-word;'>\nlorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. \n</div>\n\n### Dropdown\n\n<details markdown='1'><summary>Click to Expand/Collapse</summary>\n\nExpanded content. Shows up and keeps visible when clicking expand. Hide again by clicking the dropdown button again.\n\n</details>\n\n\n<!-- --------------- -->\n\n## Multimedia\n\n### Images\n![Image](https://gsantner.net/assets/blog/img/markor/markor-v1-7-showcase-3.jpg)\n\n### Videos\n**Youtube** [Welcome to Upper Austria](https://www.youtube.com/watch?v=RJREFH7Lmm8)\n<iframe width='360' height='200' src='https://www.youtube.com/embed/RJREFH7Lmm8'> </iframe>\n\n**Peertube** [Road in the wood](https://open.tube/videos/watch/8116312a-dbbd-43a3-9260-9ea6367c72fc)\n<div><video controls><source src='https://peertube.mastodon.host/download/videos/8116312a-dbbd-43a3-9260-9ea6367c72fc-480.mp4' </source></video></div>\n\n<!-- **Local video** <div><video controls><source src='voice-parrot.mp4' </source></video></div> -->\n\n### Audio & Music\n**Web audio** [Guifrog - Xia Yu](https://www.freemusicarchive.org/music/Guifrog/Xia_Yu)\n<audio controls src='https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Guifrog/Xia_Yu/Guifrog_-_Xia_Yu.mp3'></audio>\n\n**Local audio** Yellowcard - Lights up in the sky\n<audio controls src='../Music/mp3/Yellowcard/[2007]%20Paper%20Walls/Yellowcard%20-%2005%20-%20Light%20Up%20the%20Sky.mp3'></audio>\n\n------------------\n\nThis Markdown reference file was created for the [Markor](https://gsantner.net/project/markor?source=markdownref) project by [Gregor Santner](https://gsantner.net) and is licensed [Creative Commons Zero 1.0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) (public domain). File revision 2.\n\n------------------\n\n\n";
                break;
            }
            case 2: {
                t = "(A) Call Mom @mobile +family\n(A) Schedule annual checkup +health\n(A) Urgently buy milk @shop\n(B) Outline chapter 5 +novel @computer\n(C) Add cover sheets @work +myproject\nPlan backyard herb garden @home\nBuy salad @shop\nWrite blog post @pc\nInstall Markor @mobile\n2019-06-24 scan photos @home +blog\n2019-06-25 draw diagram @work \nx This has been done @home +renovations";
                break;
            }
            case 3: {
                t = "---\nlayout: post\ntags: []\ncategories: []\n#date: 2019-06-25 13:14:15\n#excerpt: ''\n#image: 'BASEURL/assets/blog/img/.png'\n#description:\n#permalink:\ntitle: 'title'\n---\n\n\n";
                break;
            }
            case 4: {
                t = "# Title\n## Description\n\n![Text](picture.png)\n\n### Ingredients\n\n|  Ingredient   | Amount |\n|:--------------|:-------|\n| 1             | 1      |\n| 2             | 2      |\n| 3             | 3      |\n| 4             | 4      |\n\n\n### Preparation\n\n1. Text\n2. Text\n\n";
                break;
            }
            case 5: {
                t = "---\nclass: beamer\n---\n\n-----------------\n# Cool presentation\n\n## Abed Nadir\n\n{{ post.date_today }}\n\n<!-- Overall slide design -->\n<style>\n.slide {\nbackground:url() no-repeat center center fixed; background-size: cover;\n}\n.slide_type_title {\nbackground: slategrey;\n}\n</style>\n\n-----------------\n\n## Slide title\n\n\n1. All Markdown features of Markor are **supported** for Slides too ~~strikeout~~ _italic_ `code`\n2. Start new slides with 3 more hyphens (---) separated by empty lines\n3. End last slide with hyphens too\n4. Slide backgrounds can be configured using CSS, for all and individual slides\n5. Print / PDF export in landscape mode\n6. Create title only slides (like first slide) by starting the slide (line after `---`) with title `# title`\n\n\n-----------------\n## Slide with centered image\n* Images can be centered by adding 'imghcenter' in alt text & grown to page size with 'imgbig'\n* Example: `![text imghcenter imgbig text](a.jpg)`\n\n![imghcenter imgbig](file:///android_asset/img/flowerfield.jpg)\n\n\n\n\n-----------------\n## Page with gradient background\n* and a picture\n* configure background color/image with CSS .slide_p4 { } (4 = the slide page number)\n\n![pic](file:///android_asset/img/flowerfield.jpg)\n\n\n<style> .slide_p4 { background: linear-gradient(to bottom, #11998e, #38ef7d); } </style>\n\n-----------------\n## Page with image background\n* containing text and a table\n\n| Left aligned | Middle aligned | Right aligned |\n| :------------------- | :----------------------: | --------------------: |\n| Test               | Test                    | Test                |\n| Test               | Test                    | Test                |\n\n\n\n<style> \n.slide_p5 { background: url('file:///android_asset/img/schindelpattern.jpg') no-repeat center center fixed; background-size: cover; }\n.slide_p5 > .slide_body > * { color: black; }\n</style>\n\n-----------------\n";
                break;
            }
            case 6: {
                t = "Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-01-28T20:53:47+01:00\n\n====== Zim Wiki ======\nLet me try to gather a list of the formatting options Zim provides.\n\n====== Head 1 ======\n\n===== Head 2 =====\n\n==== Head 3 ====\n\n=== Head 4 ===\n\n== Head 5 ==\n\n**Bold**\n//italics//\n__marked (yellow Background)__\n~~striked~~\n\n* Unordered List\n* second item\n	* [[Sub-Item]]\n		* Subsub-Item\n			* and one more sub\n* Back to first indent level\n\n1. ordered list\n2. second item\n	a. item 2a\n		1. Item 2a1\n		2. Item 2a2\n	b. item 2b\n		1. 2b1\n			a. 2b1a\n3. an so on...\n\n[ ] Checklist\n[ ] unchecked item\n[*] checked item\n[x] crossed item\n[>] Item marked with a yellow left-to-right-arrow\n[ ] another unchecked item\n\n\nThis ist ''preformatted text'' inline.\n\n'''\nThis is a preformatted text block.\nIt spans multiple lines.\nAnd it's visually indented.\n'''\n\nWe also have _{subscript} and ^{superscript}.\n\nIt seems there is no way to combine those styles.\n//**this is simply italic**// and you can see the asterisks.\n**//This is simply bold//** and you can see the slashes.\n__**This is simply marked yellow**__ and you can see the asterisks.\n\nThis is a web link: [[https://github.com/gsantner/markor|Markor on Github]]\nLinks inside the Zim Wiki project can be made by simply using the [[Page Name]] in double square brackets.\nThis my also contain some hierarchy information, like [[Folder:Subfolder:Document Name]]\n\n\nThis zim wiki reference file was created for the [[https://gsantner.net/project/markor?source=markdownref|Markor]] project by [[https://gsantner.net|Gregor Santner]] and is licensed [[https://creativecommons.org/publicdomain/zero/1.0/legalcode|Creative Commons Zero 1.0]] (public domain). File revision 1.";
                break;
            }
            case 7: {
                t = ZimWikiTextActions.createZimWikiHeaderAndTitleContents(filename.replaceAll("(\\.((zim)|(txt)))*$", "").trim().replace(' ', '_'), new Date(), getResources().getString(R.string.created));
                break;
            }
            case 8: {
                t = "---\ntags: []\ncreated: '{{ template.timestamp_date_yyyy_mm_dd }}'\ntitle: ''\n---\n\n";
                if (basedir != null && new File(basedir.getParentFile(), ".notabledir").exists()) {
                    t = t.replace("created:", "modified:");
                }
                break;
            }
            case 9: {
                t = "source:\ncategory:\ntag:\n------------\n";
                break;
            }
            case 10: {
                t = "Title: Txt2tags sample\nAuthor\n2021-01-31\n\n\n\nThis text is before the introduction.\n\n% this text is a secret comment (it won't be converted)\n\n\n=  Introduction  =\n\nWelcome to the txt2tags sample file.\n\nHere you have examples and a brief explanation of all\nmarks.\n\nThe first 3 lines of this file are used as headers,\non the following format:\n```\nline1: document title\nline2: author name, email\nline3: date, version\n```\n\nLines with balanced equal signs = around are titles.\n\n% a secret comment!\n%TODO link to program site http://txt2tags.org\n\n\n=  Fonts and Beautifiers  =\n\nWe have two sets of fonts:\n\nThe NORMAL type that can be improved with beautifiers.\n\nThe TYPEWRITER type that uses monospaced font for\npre-formatted text.\n\nWe will now enter on a subtitle...\n\n\n==  Beautifiers  ==\n\nThe text marks for beautifiers are simple, just as you\ntype on a plain text email message.\n\nWe use double *, /, - and _ to represent **bold** \n//italic// --strike-- and __underline__ \n\nThe **//bold italic//** style is also supported as a\ncombination.\n\n\n==  Pre-Formatted Text  ==\n\nWe can put a code sample or other pre-formatted text:\n```\n  here    is     pre-formatted __text__.\n//Marks// are  **not**  ``interpreted`` --here--.\n```\n\nAnd also, it's easy to put a one line pre-formatted\ntext:\n``` prompt$ ls /etc\n\nOr use ``pre-formatted`` inside sentences.\n\n\n==  More Cosmetics  ==\n\nSpecial entities like email (duh@somewhere.com) and\nURL (http://www.duh.com) are detected automagically,\nas long as the horizontal line:\n\n--------------------------------------------------------\n^ thin or large v\n========================================================\n\nYou can also specify an [explicit link http://duh.org]\nor an [explicit email duh@somewhere.com] with label.\n\nAnd remember,\n	A TAB in front of the line does a quotation.\n		More TABs, more depth (if allowed).\nNice.\n\n\n=  Lists  =\n\nA list of items is natural, just putting a **dash** or\na **plus** at the beginning of the line.\n\n\n==  Plain List  ==\n\nThe dash is the default list identifier. For sublists,\njust add **spaces** at the beginning of the line. More\nspaces, more sublists.\n\n- Earth\n  - America\n    - South America\n      - Brazil\n        - How deep can I go?\n  - Europe\n    - Lots of countries\n- Mars\n  - Who knows?\n\n\nThe list ends with **two** consecutive blank lines.\n\n\n==  Numbered List  ==\n\nThe same rules as the plain list, just a different\nidentifier (plus).\n\n+ one\n+ two\n+ three\n  - mixed lists!\n  - what a mess\n    + counting again\n    + ...\n+ four\n- new list (shouldn't it be an ulist instead?)\n - new list\n\n\n\n\n==  Definition List ==\n\n\n\nThe definition list identifier is a colon, followed by\nthe term. The term contents is placed on the next line.\n\n: orange\n  a yellow fruit\n: apple\n  a green or red fruit\n: other fruits\n  - wee!\n  - mixing lists\n    + again!\n    + and again!\n\n\n=  Tables  =\n\nUse pipes to compose table rows and cells.\nDouble pipe at the line beginning starts a heading row.\nNatural spaces specify each cell alignment.\n\n  | cell 1.1  |  cell 1.2   |   cell 1.3 |\n  | cell 2.1  |  cell 2.2   |   cell 2.3 |\n  | cell 3.1  |  cell 3.2   |   cell 3.3 |\n\n|| heading 1 |  heading 2  |  heading 3 |\n | cell 1.1  |  cell 1.2   |   cell 1.3 |\n | cell 2.1  |  cell 2.2   |   cell 2.3 |\n\n |_ heading 1 |  cell 1.1   |   cell 1.2 |\n  | heading 2 |  cell 2.1   |   cell 2.2 |\n  | heading 3 |  cell 3.1   |   cell 3.2 |\n\n|/ heading   |  heading 1  |  heading 2 |\n | heading 1 |  cell 1.1   |   cell 1.2 |\n | heading 2 |  cell 2.1   |   cell 2.2 |\n\n\n\n=  Special Entities  =\n\nBecause things were too simple.\n\n\n==  Images  ==\n\nThe image mark is as simple as it can be: ``[filename]``.\n\n\n[logo_txt2tags.png]  \n\nAnd with some targets the image is linkable :\n\n\n[[logo_txt2tags.png] http://www.txt2tags.org]  \n\n- The filename must end in PNG, JPG, GIF, or similar.\n- No spaces inside the brackets!\n\n\n==  Other  ==\n\nWhen the target needs, special chars like <, > and &\nare escaped.\n\nThe handy ``%%date`` macro expands to the current date.\n\nSo today is %%date on the ISO ``YYYYMMDD`` format.\n\nYou can also specify the date format with the %? flags,\nas ``%%date(%m-%d-%Y)`` which gives: %%date(%m-%d-%Y).\n\nThat's all for now.\n\n-------------------------------------------------------\n\n\n\n== Heading level 2 ==\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a diam lectus. Sed sit amet ipsum mauris. Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit. Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue. Nam tincidunt congue enim, ut porta lorem lacinia consectetur. Donec ut libero sed arcu vehicula ultricies a non tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean ut gravida lorem. Ut turpis felis, pulvinar a semper sed, adipiscing id dolor. Pellentesque auctor nisi id magna consequat sagittis. Curabitur dapibus enim sit amet elit pharetra tincidunt feugiat nisl imperdiet. \n\n\n=== Heading 3 ===\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a diam lectus. Sed sit amet ipsum mauris. Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit. Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue. Nam tincidunt congue enim, ut porta lorem lacinia consectetur. Donec ut libero sed arcu vehicula ultricies a non tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean ut gravida lorem. Ut turpis felis, pulvinar a semper sed, adipiscing id dolor. Pellentesque auctor nisi id magna consequat sagittis. Curabitur dapibus enim sit amet elit pharetra tincidunt feugiat nisl imperdiet. \n\n==== Heading 4  ====\n\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a diam lectus. Sed sit amet ipsum mauris. Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit. Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue. Nam tincidunt congue enim, ut porta lorem lacinia consectetur. Donec ut libero sed arcu vehicula ultricies a non tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean ut gravida lorem. Ut turpis felis, pulvinar a semper sed, adipiscing id dolor. Pellentesque auctor nisi id magna consequat sagittis. Curabitur dapibus enim sit amet elit pharetra tincidunt feugiat nisl imperdiet. \n\n===== Heading 5 =====\n\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a diam lectus. Sed sit amet ipsum mauris. Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit. Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue. Nam tincidunt congue enim, ut porta lorem lacinia consectetur. Donec ut libero sed arcu vehicula ultricies a non tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean ut gravida lorem. Ut turpis felis, pulvinar a semper sed, adipiscing id dolor. Pellentesque auctor nisi id magna consequat sagittis. Curabitur dapibus enim sit amet elit pharetra tincidunt feugiat nisl imperdiet. \n\n";
                break;
            }
            default:
            case 0: {
                return null; // Empty file template (that doesn't overwrite anything
            }
        }
        t = t.replace("{{ template.timestamp_date_yyyy_mm_dd }}", TodoTxtTask.DATEF_YYYY_MM_DD.format(new Date()));

        if (encrypt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bytes = new JavaPasswordbasedCryption(Build.VERSION.SDK_INT, new SecureRandom()).encrypt(t, new PasswordStore(getContext()).loadKey(R.string.pref_key__default_encryption_password));
        } else {
            bytes = t.getBytes();
        }
        return bytes;
    }
}
