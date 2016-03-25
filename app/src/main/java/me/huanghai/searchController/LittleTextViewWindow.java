package me.huanghai.searchController;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.nakardo.atableview.uikit.UILabel;

import java.util.Map;

import me.huanghai.shanghanlun_android.R;

/**
 * Created by hh on 16/3/24.
 */
public class LittleTextViewWindow extends LittleWindow {

    private View view;
    private ViewGroup mGroup;

    private String yao;
    private SpannableStringBuilder attributedString;

    private Rect rect;
    private String tag = "littleWindow";

    public String getSearchString() {
        return yao;
    }

    public void setAttributedString(SpannableStringBuilder attributedString) {
        this.attributedString = attributedString;
    }

    public LittleTextViewWindow() {
    }

    public void show(FragmentManager manager) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.addToBackStack(tag);
        ft.commit();
        SingletonData.getInstance().littleWindowStack.add(this);
    }

    public void dismiss() {
        getFragmentManager().popBackStack();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(this);
        ft.commit();
        SingletonData.getInstance().littleWindowStack.remove(this);
    }

    public void setYao(String yao) {
        SingletonData single = SingletonData.getInstance();
        Map<String, String> fangDict = single.getYaoAliasDict();
        String right = fangDict.get(yao);
        if (right == null) {
            right = yao;
        }
        this.yao = right;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public boolean isInYaoContext() {
        SpannableStringBuilder builder = attributedString;
        ClickableSpan[] spans = builder.getSpans(0,
                builder.length(), ClickableSpan.class);
        Map<String, String> dict = SingletonData.getInstance().getYaoAliasDict();
        if (spans.length > 0) {
            ClickableSpan span = spans[0];
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            String unit = builder.subSequence(start, end)
                    .toString();
            String left = dict.get(unit);
            if (left == null) {
                left = unit;
            }
            if (SingletonData.getInstance().getAllYao().contains(left)
                    && start > 0
                    && builder.toString().substring(start - 1, start).equals("、")) {
                return true;
            }
        }

        return false;
    }

    protected boolean onlyShowRelatedFang() {
        SingletonData single = SingletonData.getInstance();
        Map<String, String> yaoDict = single.getYaoAliasDict();
        String right = yao;
        if (single.littleWindowStack.size() > 0) {
            LittleWindow window = single.littleWindowStack.get(single.littleWindowStack.size() - 1);
            String text_ = window.getSearchString();
            String text = yaoDict.get(text_);
            if (text == null) {
                text = text_;
            }
            if (text.equals(right) && isInYaoContext()) {
                return true;
            }
        } else {
            ShowFragment showFragment = single.curFragment;
            if (showFragment != null && showFragment.getIsContentOpen() == true) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.show_yao, null);
        mGroup = (ViewGroup) getActivity().getWindow().getDecorView();
        mGroup.addView(view);
        Button btn = (Button) view
                .findViewById(R.id.maskbtnYao);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dismiss();
            }
        });

        UILabel textView = (UILabel) view
                .findViewById(R.id.textview);
        textView.setText("未找到");
        String s = yao != null ? yao : "";
        SpannableStringBuilder spanString = new SpannableStringBuilder();
        SingletonData single = SingletonData.getInstance();
        Map<String, String> yaoDict = single.getYaoAliasDict();
        String right = yaoDict.get(s);
        if (right == null) {
            right = s;
        }

        if (!onlyShowRelatedFang()) {
            for (HH2SectionData sec : single.getYaoData()) {
                for (DataItem item : sec.getData()) {
                    String yao__ = item.getYaoList()[0];
                    String left = yaoDict.get(yao__);
                    if (left == null) {
                        left = yao__;
                    }
                    if (left.equals(right)) {
                        spanString.append(item.getAttributedText());
                    }
                }
            }
            spanString.append("\n\n");
        }

        int count = 0;
        int index = 0;
        for (HH2SectionData sec : SingletonData.getInstance()
                .getFang()) {
            SpannableStringBuilder spanIn = new SpannableStringBuilder();
            count = 0;
            for (DataItem item : sec.getData()) {
                for (String string : item.getYaoList()) {
                    String left = yaoDict.get(string);
                    if (left == null) {
                        left = string;
                    }
                    if (left.equals(right)) {
                        count++;
                        spanIn.append(Helper.renderText("$f{"
                                + item.getFangList()[0] + "}、"));
                        break;
                    }
                }
            }
            if (index > 0) {
                spanString.append("\n\n");
            }
            spanString.append(sec.getHeader()
                    + " 凡" + count + "方");
            if (count > 0) {
                spanString.append("\n");
                spanString.append(spanIn);
            }
            index++;
        }

        textView.setText(spanString);
        textView.setMovementMethod(LocalLinkMovementMethod
                .getInstance());
        ArrowView arrow = (ArrowView) view.findViewById(R.id.arrow);

        int screenHeight = mGroup.getHeight();
        int screenWidth = mGroup.getWidth();
        int margin = Math.min(50, screenWidth / 18);
        int border = margin;
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(border, border);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int midY = rect.top + rect.height() / 2;
        int midX = rect.left + rect.width() / 2;
        int offset = 3;
        if (midY < screenHeight / 2.0) {
            params.setMargins(margin,
                    rect.top + rect.height() + border, margin, margin);
            arrowParams.setMargins(
                    midX - border / 2,
                    rect.top + rect.height() + offset,
                    screenWidth - midX - border / 2,
                    screenHeight - rect.top - rect.height() - border - offset);
            arrow.setDirection(ArrowView.UP);
        } else {
            params.gravity = Gravity.BOTTOM;
            Rect dispRect = new Rect();
            mGroup.getWindowVisibleDisplayFrame(dispRect);
            int top = dispRect.top;
            params.setMargins(
                    margin,
                    top + 8,
                    margin,
                    (screenHeight - rect.top) + border);
            arrowParams.setMargins(
                    midX - border / 2,
                    rect.top - border - offset,
                    screenWidth - midX - border / 2,
                    screenHeight - rect.top + offset);
            arrow.setDirection(ArrowView.DOWN);
        }
        ScrollView scroll = (ScrollView) view
                .findViewById(R.id.maskscroll);
        scroll.setLayoutParams(params);
        arrow.setLayoutParams(arrowParams);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        mGroup.removeView(view);
        super.onDestroyView();
    }
}
