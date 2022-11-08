package ca.uwaterloo.cs349.pdfreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so read this carefully.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;
    private int currentPageNum = 0;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    //buttons
    ToggleButton draw;
    ToggleButton eraser;
    ToggleButton highlight;
    Button pageUp;
    Button pageDown;
    TextView pageIndex;
    Button undo;
    Button redo;

    LinearLayout layout;

    ArrayList<PDFimage> pdfImages = new ArrayList();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restore value of members from saved state
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            openRenderer(this);
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }

        draw = findViewById(R.id.buttonDraw);
        eraser = findViewById(R.id.buttonEraser);
        highlight = findViewById(R.id.buttonHighlight);

        undo = findViewById(R.id.undoBtn);
        redo = findViewById(R.id.redoBtn);

        pageUp = findViewById(R.id.pageUpBtn);
        pageDown = findViewById(R.id.pageDownBtn);

        pageIndex = findViewById(R.id.pageNumber);

        draw.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    highlight.setChecked(false);
                    eraser.setChecked(false);
                }
                Log.d(LOGNAME, pdfImages.get(0).paths.toString());
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (eraser.isChecked()){
                    draw.setChecked(false);
                    highlight.setChecked(false);
                }
                //Log.d(LOGNAME, "in ActivityMain paths size is: " + pdfImages.get(0).paths.size());

            }
        });

        highlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (highlight.isChecked()) {
                    eraser.setChecked(false);
                    draw.setChecked(false);
                }

            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pdfImages.get(currentPageNum).undoStack.size() == 0) {
                    Log.d(LOGNAME, "undoStack size is: " + pdfImages.get(currentPageNum).undoStack.size());
                    Log.d(LOGNAME, "paths size is: " + pdfImages.get(currentPageNum).paths.size());
                    return;
                }

                Pair<String, Pair<ArrayList<Path>, ArrayList<Path>>> info = pdfImages.get(currentPageNum).undoStack.pop();
                String s = info.first;
                //String s = pdfImages.get(currentPageNum).undoStack.get(pdfImages.get(currentPageNum).undoStack.size()-1);
                Log.d(LOGNAME, "pop off msg : " + s);

                if (s.equals("h")) {
                    int size = pdfImages.get(currentPageNum).hPaths.size();
                    Path temp = pdfImages.get(currentPageNum).hPaths.get(size - 1);
                    pdfImages.get(currentPageNum).hPaths.remove(size - 1);
                    ArrayList<Path> highlightArr = new ArrayList<>();
                    highlightArr.add(temp);
                    pdfImages.get(currentPageNum).redoStack.push(new Pair<>("h", new Pair<>(info.second.first, highlightArr)));
                } else if (s.equals("d")) {
                    int size2 = pdfImages.get(currentPageNum).paths.size();
                    Path temp2 = pdfImages.get(currentPageNum).paths.get(size2 - 1);
                    ArrayList<Path> drawArr = new ArrayList<>();
                    drawArr.add(temp2);
                    //info.second.first.add(temp2);
                    pdfImages.get(currentPageNum).paths.remove(size2 - 1);
                    pdfImages.get(currentPageNum).redoStack.push(new Pair<>("d", new Pair<>(drawArr, info.second.second)));
                } else {
                    //Path temp3 = pdfImages.get(currentPageNum).ePaths.get(s);
                    //pdfImages.get(currentPageNum).ePaths.remove(s);
                    for (Path p : info.second.first) {
                        pdfImages.get(currentPageNum).paths.add(p);
                    }
                    for (Path p : info.second.second) {
                        pdfImages.get(currentPageNum).hPaths.add(p);
                    }
                    pdfImages.get(currentPageNum).redoStack.push(info);
                }
            }
        });


        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pdfImages.get(currentPageNum).redoStack.size() == 0) {
                    return;
                }
                Pair<String, Pair<ArrayList<Path>, ArrayList<Path>>> info = pdfImages.get(currentPageNum).redoStack.pop();
                String s = info.first;
                if (s.equals("h")) {
                    pdfImages.get(currentPageNum).hPaths.add(info.second.second.get(0));
                    pdfImages.get(currentPageNum).undoStack.push(info);
                } else if (s.equals("d")) {
                    pdfImages.get(currentPageNum).paths.add(info.second.first.get(0));
                    pdfImages.get(currentPageNum).undoStack.push(info);
                } else {
                    //pdfImages.get(currentPageNum).ePaths.put(s, p.second);
                    int sizeD = info.second.first.size();
                    int sizeH = info.second.second.size();

                    int pathSize = pdfImages.get(currentPageNum).paths.size();
                    for (int i = pathSize - 1; i >= pathSize - sizeD; i--) {
                        pdfImages.get(currentPageNum).paths.remove(i);
                    }

                    int hPathSize = pdfImages.get(currentPageNum).hPaths.size();
                    for (int i = hPathSize - 1; i >= hPathSize - sizeH; i--) {
                        pdfImages.get(currentPageNum).hPaths.remove(i);
                    }

                    pdfImages.get(currentPageNum).undoStack.push(info);
                }
            }
        });

        for (int i = 0; i < 55; i++) {
            pageImage = new PDFimage(this, draw, eraser, highlight, undo, redo);
            pageImage.setMinimumWidth(1000);
            pageImage.setMinimumHeight(2000);
            PdfRenderer.Page page = pdfRenderer.openPage(i);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageImage.setImage(bitmap);
            page.close();

            pdfImages.add(pageImage);
        }

        layout = findViewById(R.id.pdfLayout);
        //pageImage = new PDFimage(this, draw, eraser, highlight);
        layout.addView(pdfImages.get(0));
        layout.setEnabled(true);
        //pdfImages.get(0).setMinimumWidth(1000);
        //pdfImages.get(0).setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        /*try {
            openRenderer(this);
            showPage(0);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }*/


        pageUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPageNum != 0) {
                    layout.removeView(pdfImages.get(currentPageNum));
                    currentPageNum--;
                    pageIndex.setText((currentPageNum+1)+"/55");
                    //showPage(currentPageNum);
                    layout.addView(pdfImages.get(currentPageNum));


                }
            }
        });

        pageDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPageNum != 54) {
                    layout.removeView(pdfImages.get(currentPageNum));
                    currentPageNum++;
                    pageIndex.setText((currentPageNum+1)+"/55");
                    //showPage(currentPageNum);
                    layout.addView(pdfImages.get(currentPageNum));
                }
            }
        });
    }

    /*@Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {

        //Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("arraylist", pdfImages);


        super.onSaveInstanceState(bundle);

    }*/

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (null != currentPage) {
                currentPage.close();
            }
            pdfRenderer.close();
            parcelFileDescriptor.close();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        Log.d(LOGNAME,"a");
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        Log.d(LOGNAME,"b");
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        Log.d(LOGNAME,"c");
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        Log.d(LOGNAME,"d");

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        Log.d(LOGNAME,"e");

        // Display the page
        pageImage.setImage(bitmap);
    }*/
}