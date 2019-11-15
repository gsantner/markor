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
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "net.gsantner.markor.ui.NewFileDialog";
    public static final String EXTRA_DIR = "EXTRA_DIR";
    private Callback.a2<Boolean, File> callback;

    public static NewFileDialog newInstance(File sourceFile, Callback.a2<Boolean, File> callback) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private AlertDialog.Builder makeDialog(final File basedir, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template);
        final String[] typeSpinnerToExtension = getResources().getStringArray(R.array.new_file_types__file_extension);

        fileNameEdit.requestFocus();
        new Handler().postDelayed(new ContextUtils.DoTouchView(fileNameEdit), 200);

        fileNameEdit.setFilters(new InputFilter[]{ContextUtils.INPUTFILTER_FILENAME});
        fileExtEdit.setFilters(fileNameEdit.getFilters());

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String prefix = null;
                String ext = i < typeSpinnerToExtension.length ? typeSpinnerToExtension[i] : "";
                switch (i) {
                    case 4: {
                        prefix = new SimpleDateFormat("yyyy-MM-dd-").format(new Date());
                        break;
                    }
                }

                if (ext != null) {
                    fileExtEdit.setText(ext);
                }
                if (prefix != null && !fileNameEdit.getText().toString().startsWith(prefix)) {
                    fileNameEdit.setText(prefix + fileNameEdit.getText().toString());
                }
                fileNameEdit.setSelection(fileNameEdit.length());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();
        if (BuildConfig.IS_TEST_BUILD) {
            fileNameEdit.setText("a");
            templateSpinner.postDelayed(() -> {
                templateSpinner.requestFocus();
                templateSpinner.performClick();
            }, 100);
        }

        final ShareUtil shareUtil = new ShareUtil(getContext());
        dialogBuilder
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }

                    File f = new File(basedir, fileNameEdit.getText().toString() + fileExtEdit.getText().toString());
                    String templateContents = getTemplateContent(templateSpinner, basedir);
                    if (shareUtil.isUnderStorageAccessFolder(f)) {
                        DocumentFile dof = shareUtil.getDocumentFile(f, false);
                        if (f.exists() && f.length() < 5 && templateContents != null) {
                            shareUtil.writeFile(f, false, (fileOpened, fos) -> {
                                try {
                                    fos.write(templateContents.getBytes());
                                } catch (Exception ignored) {
                                }
                            });
                        }
                        callback(dof != null && dof.canWrite(), f);
                    } else {
                        boolean touchOk = FileUtils.touch(f);
                        if (f.exists() && (f.length() < 5 || BuildConfig.IS_TEST_BUILD) && templateContents != null) {
                            FileUtils.writeFile(f, templateContents);
                        }
                        callback(touchOk || f.exists(), f);
                    }
                    dialogInterface.dismiss();
                })
                .setNeutralButton(R.string.folder, (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }
                    File f = new File(basedir, fileNameEdit.getText().toString());
                    if (shareUtil.isUnderStorageAccessFolder(f)) {
                        DocumentFile dof = shareUtil.getDocumentFile(f, true);
                        callback(dof != null && dof.exists(), f);
                    } else {
                        callback(f.mkdirs() || f.exists(), f);
                    }
                    dialogInterface.dismiss();
                });

        return dialogBuilder;
    }

    private boolean ez(EditText et) {
        return et.getText().toString().isEmpty();
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
    private String getTemplateContent(final Spinner templateSpinner, final File basedir) {
        String t = null;
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
                t = "---\ntags: []\ncreated: '{{ template.timestamp_date_yyyy_mm_dd }}'\ntitle: ''\n---\n\n";
                if (basedir != null && new File(basedir.getParentFile(), ".notabledir").exists()) {
                    t = t.replace("created:", "modified:");
                }
                break;
            }
            default:
            case 0: {
                return null; // Empty file template (that doesn't overwrite anything
            }
        }
        return t.replace("{{ template.timestamp_date_yyyy_mm_dd }}", SttCommander.DATEF_YYYY_MM_DD.format(new Date()));
    }
}
