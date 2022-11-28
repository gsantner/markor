/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.format.wikitext.WikitextActionButtons;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsAndroidSpinnerOnItemSelectedAdapter;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = NewFileDialog.class.getName();
    public static final String EXTRA_DIR = "EXTRA_DIR";
    public static final String EXTRA_ALLOW_CREATE_DIR = "EXTRA_ALLOW_CREATE_DIR";
    private GsCallback.a2<Boolean, File> callback;

    public static NewFileDialog newInstance(final File sourceFile, final boolean allowCreateDir, final GsCallback.a2<Boolean, File> callback) {
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
        final AppSettings appSettings = ApplicationObject.settings();
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.Theme_AppCompat_DayNight_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final CheckBox encryptCheckbox = root.findViewById(R.id.new_file_dialog__encrypt);
        final CheckBox utf8BomCheckbox = root.findViewById(R.id.new_file_dialog__utf8_bom);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template);
        final String[] typeSpinnerToExtension = getResources().getStringArray(R.array.new_file_types__file_extension);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && appSettings.isDefaultPasswordSet()) {
            encryptCheckbox.setChecked(appSettings.getNewFileDialogLastUsedEncryption());
        } else {
            encryptCheckbox.setVisibility(View.GONE);
        }
        utf8BomCheckbox.setChecked(appSettings.getNewFileDialogLastUsedUtf8Bom());
        utf8BomCheckbox.setVisibility(appSettings.isExperimentalFeaturesEnabled() ? View.VISIBLE : View.GONE);
        fileExtEdit.setText(appSettings.getNewFileDialogLastUsedExtension());
        fileNameEdit.requestFocus();
        new Handler().postDelayed(new GsContextUtils.DoTouchView(fileNameEdit), 200);

        fileNameEdit.setFilters(new InputFilter[]{GsContextUtils.instance.makeFilenameInputFilter()});
        fileExtEdit.setFilters(fileNameEdit.getFilters());

        loadTemplatesIntoSpinner(appSettings, templateSpinner);
        final AtomicBoolean typeSpinnerNoTriggerOnFirst = new AtomicBoolean(true);
        typeSpinner.setOnItemSelectedListener(new GsAndroidSpinnerOnItemSelectedAdapter(pos -> {
            if (pos == 3) { // Wikitext
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

        templateSpinner.setOnItemSelectedListener(new GsAndroidSpinnerOnItemSelectedAdapter(pos -> {
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

        utf8BomCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appSettings.setNewFileDialogLastUsedUtf8Bom(isChecked);
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();

        final MarkorContextUtils cu = new MarkorContextUtils(getContext());
        dialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            if (ez(fileNameEdit)) {
                return;
            }

            appSettings.setNewFileDialogLastUsedExtension(fileExtEdit.getText().toString().trim());
            final String usedFilename = getFileNameWithoutExtension(fileNameEdit.getText().toString(), templateSpinner.getSelectedItemPosition());
            final File f = new File(basedir, Document.normalizeFilename(usedFilename.trim()) + fileExtEdit.getText().toString().trim());
            final Pair<byte[], Integer> templateContents = getTemplateContent(templateSpinner, basedir, f.getName(), encryptCheckbox.isChecked());
            cu.writeFile(getActivity(), f, false, (arg_ok, arg_fos) -> {
                try {
                    if (appSettings.getNewFileDialogLastUsedUtf8Bom()) {
                        arg_fos.write(0xEF);
                        arg_fos.write(0xBB);
                        arg_fos.write(0xBF);
                    }
                    if (templateContents.first != null && (!f.exists() || f.length() < GsContextUtils.TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH)) {
                        arg_fos.write(templateContents.first);
                    }
                } catch (Exception ignored) {
                }
                if (templateContents.second >= 0) {
                    appSettings.setLastEditPosition(f.getAbsolutePath(), templateContents.second);
                }
                callback(arg_ok || f.exists(), f);
                dialogInterface.dismiss();
            });
        });

        dialogBuilder.setNeutralButton(R.string.folder, (dialogInterface, i) -> {
            if (ez(fileNameEdit)) {
                return;
            }
            final String usedFoldername = getFileNameWithoutExtension(fileNameEdit.getText().toString().trim(), templateSpinner.getSelectedItemPosition());
            File f = new File(basedir, usedFoldername);
            if (cu.isUnderStorageAccessFolder(getContext(), f, true)) {
                DocumentFile dof = cu.getDocumentFile(getContext(), f, true);
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

    private void loadTemplatesIntoSpinner(final AppSettings appSettings, final Spinner templateSpinner) {
        List<String> templates = new ArrayList<>();
        for (int i = 0; i < templateSpinner.getCount(); i++) {
            templates.add((String) templateSpinner.getAdapter().getItem(i));
        }
        templates.addAll(MarkorDialogFactory.getSnippets(appSettings).keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, templates.toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        templateSpinner.setAdapter(adapter);
    }

    private boolean ez(EditText et) {
        return et.getText().toString().trim().isEmpty();
    }

    private String getFileNameWithoutExtension(String typedFilename, int selectedTemplatePos) {
        if (selectedTemplatePos == 7) {
            // Wikitext files always use underscores instead of spaces
            return typedFilename.trim().replace(' ', '_');
        }
        return typedFilename.trim();
    }

    private void callback(boolean ok, File file) {
        try {
            callback.callback(ok, file);
        } catch (Exception ignored) {
        }
    }

    // How to get content out of a file:
    // 1) Replace \n with \n | copy to clipboard
    //    cat markor-markdown-reference.md  | sed 's@\\@\\\\@g' | sed -z 's@\n@\n@g'  | xclip
    //
    // 2) t = "<cursor>";  | ctrl+shift+v "paste without formatting"
    //
    @SuppressLint("TrulyRandom")
    private Pair<byte[], Integer> getTemplateContent(final Spinner templateSpinner, final File basedir, final String filename, final boolean encrypt) {
        String t = null;
        switch (templateSpinner.getSelectedItemPosition()) {
            case 1: {
                t = "# Markdown Reference\nAutomatically generate _table of contents_ by checking the option here: `Settings > Format > Markdown`.\n\n## H2 Header\n### H3 header\n#### H4 Header\n##### H5 Header\n###### H6 Header\n\n<!-- --------------- -->\n\n## Format Text\n\n*Italic emphasis* , _Alternative italic emphasis_\n\n**Bold emphasis** , __Alternative bold emphasis__\n\n~~Strikethrough~~\n\nBreak line (two spaces at end of line)  \n\n> Block quote\n\n`Inline code`\n\n```\nCode blocks\nare\nawesome\n```\n\n<!-- --------------- -->\n \n## Lists\n### Ordered & unordered\n\n* Unordered list\n* ...with asterisk/star\n* Test\n\n- Another unordered list\n- ...with hyphen/minus\n- Test\n\n1. Ordered list\n2. Test\n3. Test\n4. Test\n\n- Nested lists\n    * Unordered nested list\n    * Test\n    * Test\n    * Test\n- Ordered nested list\n    1. Test\n    2. Test\n    3. Test\n    4. Test\n- Double-nested unordered list\n    - Test\n    - Unordered\n        - Test a\n        - Test b\n    - Ordered\n        1. Test 1\n        2. Test 2\n\n### Checklist\n* [ ] Salad\n* [x] Potatoes\n\n1. [x] Clean\n2. [ ] Cook\n\n<!-- --------------- -->\n\n## Links\n[Link](https://duckduckgo.com/)\n\n[File in same folder as the document.](markor-markdown-reference.md) Use %20 for spaces!\n\n<!-- --------------- -->\n\n## Tables\n\n| Left aligned | Middle aligned | Right aligned |\n| :--------------- | :------------------: | -----------------: |\n| Test                 | Test                      | Test                    |\n| Test                 | Test                      | Test                    |\n\n÷÷÷÷\n\nShorter | Table | Syntax\n:---: | ---: | :---\nTest | Test | Test\nTest | Test | Test\n\n<!-- Comment: Not visibile in view. Can also span across multiple lines. End with:-->\n\n<!-- ------------- -->\n\n## Math (KaTeX)\nSee [reference](https://katex.org/docs/supported.html) & [examples](https://github.com/waylonflinn/markdown-it-katex/blob/master/README.md). Enable by checking Math at `Settings > Markdown`.\n\n### Math inline\n\n$ I = \\frac V R $\n\n### Math block\n\n$$\\begin{array}{c} \nabla \\times \\vec{\\mathbf{B}} -\\, \\frac1c\\, \\frac{\\partial\\vec{\\mathbf{E}}}{\\partial t} & = \\frac{4\\pi}{c}\\vec{\\mathbf{j}} \nabla \\cdot \\vec{\\mathbf{E}} & = 4 \\pi \\rho \\\\ \nabla \\times \\vec{\\mathbf{E}}\\, +\\, \\frac1c\\, \\frac{\\partial\\vec{\\mathbf{B}}}{\\partial t} & = \\vec{\\mathbf{0}} \\\\ \nabla \\cdot \\vec{\\mathbf{B}} & = 0 \\end{array}$$\n\n\n$$\\frac{k_t}{k_e} = \\sqrt{2}$$\n\n<!-- ------------- -->\n\n## Format Text (continued)\n\n### Text color\n\n<span style='background-color:#ffcb2e;'>Text with background color / highlight</span>\n\n<span style='color:#3333ff;'>Text foreground color</span>\n\n<span style='text-shadow: 0px 0px 2px #FF0000;'>Text with colored outline</span> / <span style='text-shadow: 0px 0px 2px #0000FF; color: white'>Text with colored outline</span>\n\n\n### Text sub & superscript\n\n<u>Underline</u>\n\nThe <sub>Subway</sub> sandwich was <sup>super</sup>\n\nSuper special characters: ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ ™ ® ℠\n\n### Text positioning\n<div markdown='1' align='right'>\n\ntext on the **right**\n\n</div>\n\n<div markdown='1' align='center'>\n\ntext in the **center**  \n(one empy line above and below  \nrequired for Markdown support OR markdown='1')\n\n</div>\n\n### Block Text\n\n<div markdown='1' style='text-align: justify; text-justify: inter-word;'>\nlorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. \n</div>\n\n### Dropdown\n\n<details markdown='1'><summary>Click to Expand/Collapse</summary>\n\nExpanded content. Shows up and keeps visible when clicking expand. Hide again by clicking the dropdown button again.\n\n</details>\n\n\n### Break page\nTo break the page (/start a new page), put the div below into a own line.\nRelevant for creating printable pages from the document (Print / PDF).\n\n<div style='page-break-after: always'></div>\n\n\n<!-- ------------- -->\n\n## Multimedia\n\n### Images\n![Image](https://gsantner.net/assets/blog/img/markor/markor-v1-7-showcase-3.jpg)\n\n### Videos\n**Youtube** [Welcome to Upper Austria](https://www.youtube.com/watch?v=RJREFH7Lmm8)\n<iframe width='360' height='200' src='https://www.youtube.com/embed/RJREFH7Lmm8'> </iframe>\n\n**Peertube** [Road in the wood](https://open.tube/videos/watch/8116312a-dbbd-43a3-9260-9ea6367c72fc)\n<div><video controls><source src='https://peertube.mastodon.host/download/videos/8116312a-dbbd-43a3-9260-9ea6367c72fc-480.mp4' </source></video></div>\n\n<!-- **Local video** <div><video controls><source src='voice-parrot.mp4' </source></video></div> -->\n\n### Audio & Music\n**Web audio** [Guifrog - Xia Yu](https://www.freemusicarchive.org/music/Guifrog/Xia_Yu)\n<audio controls src='https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Guifrog/Xia_Yu/Guifrog_-_Xia_Yu.mp3'></audio>\n\n**Local audio** Yellowcard - Lights up in the sky\n<audio controls src='../Music/mp3/Yellowcard/[2007]%20Paper%20Walls/Yellowcard%20-%2005%20-%20Light%20Up%20the%20Sky.mp3'></audio>\n\n## Charts / Graphs / Diagrams (mermaidjs)\nPie, flow, sequence, class, state, ER  \nSee also: mermaidjs [live editor](https://mermaid-js.github.io/mermaid-live-editor/).\n\n```mermaid\ngraph LR\n    A[Square Rect] -- Link text --> B((Circle))\n    A --> C(Round Rect)\n    B --> D{Rhombus}\n    C --> D\n```\n\n\n\n## Admonition Extension\nCreate block-styled side content.  \nUse one of these qualifiers to select the icon and the block color: abstract, summary, tldr, bug, danger, error, example, snippet, failure, fail, missing, question, help, faq, info, todo, note, seealso, quote, cite, success, check, done, tip, hint, important, warning, caution, attention.\n\n!!! warning 'Optional Title'\n    Block-Styled Side Content with **Markdown support**\n\n!!! info ''\n    No-Heading Content\n\n??? bug 'Collapsed by default'\n    Collapsible Block-Styled Side Content\n\n???+ example 'Open by default'\n     Collapsible Block-Styled Side Content\n\n------------------\n\nThis Markdown reference file was created for the [Markor](https://gsantner.net/project/markor?source=markdownref) project by [Gregor Santner](https://gsantner.net) and is licensed [Creative Commons Zero 1.0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) (public domain). File revision 3.\n\n------------------\n\n\n";
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
                t = WikitextActionButtons.createWikitextHeaderAndTitleContents(filename.replaceAll("(\\.((zim)|(txt)))*$", "").trim().replace(' ', '_'), new Date(), getResources().getString(R.string.created));
                break;
            }
            case 8: {
                t = TextViewUtils.interpolateEscapedDateTime("---\ntags: []\ncreated: '`yyyy-MM-dd`'\ntitle: ''\n---\n\n");
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
                t = "= My Title\n:page-subtitle: This is a subtitle\n:page-last-updated: 2029-01-01\n:page-tags: ['AsciiDoc', 'Markor', 'open source']\n:toc: auto\n:toclevels: 2\n// :page-description: the optional description\n// This should match the structure on the jekyll server:\n:imagesdir: ../assets/img/blog\n\nifndef::env-site[]\n\n// on the jekyll server, the :page-subtitle: is displayed below the title.\n// but it is not shown, when rendered in html5, and the site is rendered in html5, when working locally\n// so we show it additionally only, when we work locally\n// https://docs.asciidoctor.org/asciidoc/latest/document/subtitle/\n\n[discrete] \n=== {page-subtitle}\n\nendif::env-site[]\n\n// local testing:\n:imagesdir: ../app/src/main/assets/img\n\nimage::flowerfield.jpg[]\n\nimage::schindelpattern.jpg[Schindelpattern,200]\n\nbefore inline picture image:schindelpattern.jpg[Schindelpattern,50] and after inline picture\n";
                break;
            }
            default: {
                final Map<String, File> snippets = MarkorDialogFactory.getSnippets(ApplicationObject.settings());
                if (templateSpinner.getSelectedItem() instanceof String && snippets.containsKey((String) templateSpinner.getSelectedItem())) {
                    t = TextViewUtils.interpolateEscapedDateTime(GsFileUtils.readTextFileFast(snippets.get((String) templateSpinner.getSelectedItem())).first);
                    break;
                }
            }
        }

        if (t == null) {
            return new Pair<>(null, -1);
        }

        final int startingIndex = t.indexOf(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN);
        t = t.replace(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN, "");

        final byte[] bytes;
        if (encrypt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final char[] pass = ApplicationObject.settings().getDefaultPassword();
            bytes = new JavaPasswordbasedCryption(Build.VERSION.SDK_INT, new SecureRandom()).encrypt(t, pass);
        } else {
            bytes = t.getBytes();
        }

        return Pair.create(bytes, startingIndex);
    }
}
