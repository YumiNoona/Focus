package dev.still.fixture;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
public class ScrollActivity extends Activity {
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(48, 160, 48, 48);
        TextView text = new TextView(this); text.setText("Scroll Test\n\nA separate distraction app for checking Focus protection."); text.setTextSize(24); layout.addView(text);
        EditText input = new EditText(this); input.setHint("Type something"); layout.addView(input);
        Button button = new Button(this); button.setText("Change content"); button.setOnClickListener(view -> text.setText("Content changed without switching apps.")); layout.addView(button);
        setContentView(layout);
    }
}
