package com.timed.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.timed.R;
import com.timed.utils.RecurrenceConfig;
import com.timed.utils.RecurrenceTextFormatter;
import com.timed.utils.RecurrenceUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurrenceRuleBottomSheet extends BottomSheetDialogFragment {
    public interface OnRecurrenceConfirmedListener {
        void onConfirmed(RecurrenceConfig config);
    }

    private static final String ARG_RRULE = "arg_rrule";
    private static final String ARG_EXCEPTIONS = "arg_exceptions";
    private static final String ARG_START_TIME = "arg_start_time";
    private static final String ARG_ENABLED = "arg_enabled";

    private static final SimpleDateFormat EXCEPTION_STORE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private RecurrenceConfig config;
    private long startTimeMillis;
    private OnRecurrenceConfirmedListener listener;

    private TextView pillDaily;
    private TextView pillWeekly;
    private TextView pillMonthly;
    private TextView pillYearly;
    private TextView pillCustom;
    private TextView tvIntervalValue;
    private TextView tvIntervalUnit;
    private TextView tvWeekdayLabel;
    private TextView btnIntervalMinus;
    private TextView btnIntervalPlus;
    private View scrollWeekdays;
    private TextView chipMon;
    private TextView chipTue;
    private TextView chipWed;
    private TextView chipThu;
    private TextView chipFri;
    private TextView chipSat;
    private TextView chipSun;
    private LinearLayout layoutAdvancedRule;
    private View layoutPositionField;
    private View layoutDayField;
    private TextView tvPositionValue;
    private TextView tvDayValue;
    private TextView tvAdvancedExample;
    private LinearLayout llExceptionsContainer;
    private View btnAddException;
    private RadioGroup rgEndOptions;
    private RadioButton rbNever;
    private RadioButton rbOnDate;
    private RadioButton rbAfterCount;
    private View layoutEndDateRow;
    private TextView tvEndDateValue;
    private View layoutEndCountRow;
    private TextView btnCountMinus;
    private TextView btnCountPlus;
    private TextView tvCountValue;
    private TextView tvPreview;
    private boolean customSelected = false;

    public static RecurrenceRuleBottomSheet newInstance(RecurrenceConfig input, long startTimeMillis) {
        RecurrenceRuleBottomSheet sheet = new RecurrenceRuleBottomSheet();
        Bundle args = new Bundle();
        String rrule = input != null ? input.toRRuleString() : "";
        args.putString(ARG_RRULE, rrule);
        args.putLong(ARG_START_TIME, startTimeMillis);
        args.putBoolean(ARG_ENABLED, input != null && input.enabled);
        ArrayList<String> exceptions = new ArrayList<>();
        if (input != null && input.exceptions != null) {
            exceptions.addAll(input.exceptions);
        }
        args.putStringArrayList(ARG_EXCEPTIONS, exceptions);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnConfirmedListener(OnRecurrenceConfirmedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_recurrence_rule_sheet, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog sheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(params);

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupConfigFromArgs();
        bindViews(view);
        bindListeners();
        applyConfigToUi();
    }

    private void setupConfigFromArgs() {
        Bundle args = getArguments();
        String rrule = args != null ? args.getString(ARG_RRULE, "") : "";
        startTimeMillis = args != null ? args.getLong(ARG_START_TIME, System.currentTimeMillis())
                : System.currentTimeMillis();
        boolean enabled = args != null && args.getBoolean(ARG_ENABLED, !rrule.isEmpty());

        config = RecurrenceConfig.fromRRule(rrule);
        config.enabled = enabled || !rrule.isEmpty();
        config.applyStartDefaults(startTimeMillis);

        if (args != null) {
            ArrayList<String> exceptions = args.getStringArrayList(ARG_EXCEPTIONS);
            if (exceptions != null) {
                config.exceptions = new ArrayList<>(exceptions);
            }
        }

        if (!config.enabled) {
            config.enabled = true;
        }
    }

    private void bindViews(View view) {
        pillDaily = view.findViewById(R.id.pillDaily);
        pillWeekly = view.findViewById(R.id.pillWeekly);
        pillMonthly = view.findViewById(R.id.pillMonthly);
        pillYearly = view.findViewById(R.id.pillYearly);
        pillCustom = view.findViewById(R.id.pillCustom);
        tvIntervalValue = view.findViewById(R.id.tvIntervalValue);
        tvIntervalUnit = view.findViewById(R.id.tvIntervalUnit);
        tvWeekdayLabel = view.findViewById(R.id.tvWeekdayLabel);
        btnIntervalMinus = view.findViewById(R.id.btnIntervalMinus);
        btnIntervalPlus = view.findViewById(R.id.btnIntervalPlus);
        scrollWeekdays = view.findViewById(R.id.scrollWeekdays);
        chipMon = view.findViewById(R.id.chipMon);
        chipTue = view.findViewById(R.id.chipTue);
        chipWed = view.findViewById(R.id.chipWed);
        chipThu = view.findViewById(R.id.chipThu);
        chipFri = view.findViewById(R.id.chipFri);
        chipSat = view.findViewById(R.id.chipSat);
        chipSun = view.findViewById(R.id.chipSun);
        layoutAdvancedRule = view.findViewById(R.id.layoutAdvancedRule);
        layoutPositionField = view.findViewById(R.id.layoutPositionField);
        layoutDayField = view.findViewById(R.id.layoutDayField);
        tvPositionValue = view.findViewById(R.id.tvPositionValue);
        tvDayValue = view.findViewById(R.id.tvDayValue);
        tvAdvancedExample = view.findViewById(R.id.tvAdvancedExample);
        llExceptionsContainer = view.findViewById(R.id.llExceptionsContainer);
        btnAddException = view.findViewById(R.id.btnAddException);
        rgEndOptions = view.findViewById(R.id.rgEndOptions);
        rbNever = view.findViewById(R.id.rbNever);
        rbOnDate = view.findViewById(R.id.rbOnDate);
        rbAfterCount = view.findViewById(R.id.rbAfterCount);
        layoutEndDateRow = view.findViewById(R.id.layoutEndDateRow);
        tvEndDateValue = view.findViewById(R.id.tvEndDateValue);
        layoutEndCountRow = view.findViewById(R.id.layoutEndCountRow);
        btnCountMinus = view.findViewById(R.id.btnCountMinus);
        btnCountPlus = view.findViewById(R.id.btnCountPlus);
        tvCountValue = view.findViewById(R.id.tvCountValue);
        tvPreview = view.findViewById(R.id.tvPreview);

        View btnBack = view.findViewById(R.id.btnBack);
        View btnDone = view.findViewById(R.id.btnDone);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> confirmAndClose());
        }
    }

    private void bindListeners() {
        pillDaily.setOnClickListener(v -> selectFrequency(RecurrenceUtils.FREQ_DAILY, false));
        pillWeekly.setOnClickListener(v -> selectFrequency(RecurrenceUtils.FREQ_WEEKLY, false));
        pillMonthly.setOnClickListener(v -> selectFrequency(RecurrenceUtils.FREQ_MONTHLY, false));
        pillYearly.setOnClickListener(v -> selectFrequency(RecurrenceUtils.FREQ_YEARLY, false));
        pillCustom.setOnClickListener(v -> selectFrequency(RecurrenceUtils.FREQ_MONTHLY, true));

        btnIntervalMinus.setOnClickListener(v -> updateInterval(-1));
        btnIntervalPlus.setOnClickListener(v -> updateInterval(1));

        chipMon.setOnClickListener(v -> toggleWeekday("MO", chipMon));
        chipTue.setOnClickListener(v -> toggleWeekday("TU", chipTue));
        chipWed.setOnClickListener(v -> toggleWeekday("WE", chipWed));
        chipThu.setOnClickListener(v -> toggleWeekday("TH", chipThu));
        chipFri.setOnClickListener(v -> toggleWeekday("FR", chipFri));
        chipSat.setOnClickListener(v -> toggleWeekday("SA", chipSat));
        chipSun.setOnClickListener(v -> toggleWeekday("SU", chipSun));

        layoutPositionField.setOnClickListener(v -> showPositionPicker());
        layoutDayField.setOnClickListener(v -> showDayPicker());

        btnAddException.setOnClickListener(v -> showExceptionPicker());

        rgEndOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbNever) {
                config.endType = RecurrenceConfig.EndType.NEVER;
            } else if (checkedId == R.id.rbOnDate) {
                config.endType = RecurrenceConfig.EndType.UNTIL;
                ensureUntilDate();
            } else if (checkedId == R.id.rbAfterCount) {
                config.endType = RecurrenceConfig.EndType.COUNT;
            }
            updateEndUi();
            updatePreview();
        });

        layoutEndDateRow.setOnClickListener(v -> showEndDatePicker());
        btnCountMinus.setOnClickListener(v -> updateCount(-1));
        btnCountPlus.setOnClickListener(v -> updateCount(1));
    }

    private void applyConfigToUi() {
        selectFrequency(config.frequency, false);
        tvIntervalValue.setText(String.valueOf(config.interval));
        updateWeekdayChips();
        updateAdvancedFields();
        updateExceptions();
        updateEndUi();
        updatePreview();
    }

    private void selectFrequency(String frequency, boolean custom) {
        config.frequency = frequency;
        customSelected = custom;
        config.applyStartDefaults(startTimeMillis);

        setPillState(pillDaily, RecurrenceUtils.FREQ_DAILY.equals(frequency));
        setPillState(pillWeekly, RecurrenceUtils.FREQ_WEEKLY.equals(frequency));
        setPillState(pillMonthly, RecurrenceUtils.FREQ_MONTHLY.equals(frequency) && !customSelected);
        setPillState(pillYearly, RecurrenceUtils.FREQ_YEARLY.equals(frequency));
        setPillState(pillCustom, customSelected);

        boolean showWeekdays = RecurrenceUtils.FREQ_WEEKLY.equals(frequency);
        tvWeekdayLabel.setVisibility(showWeekdays ? View.VISIBLE : View.GONE);
        scrollWeekdays.setVisibility(showWeekdays ? View.VISIBLE : View.GONE);

        boolean showAdvanced = RecurrenceUtils.FREQ_MONTHLY.equals(frequency);
        layoutAdvancedRule.setVisibility(showAdvanced ? View.VISIBLE : View.GONE);

        updateIntervalUnit();
        updateAdvancedFields();
        updatePreview();
    }

    private void updateIntervalUnit() {
        String unit = "tuần";
        if (RecurrenceUtils.FREQ_DAILY.equals(config.frequency)) {
            unit = "ngày";
        } else if (RecurrenceUtils.FREQ_MONTHLY.equals(config.frequency)) {
            unit = "tháng";
        } else if (RecurrenceUtils.FREQ_YEARLY.equals(config.frequency)) {
            unit = "năm";
        }
        tvIntervalUnit.setText(unit);
    }

    private void updateInterval(int delta) {
        config.interval = Math.max(1, config.interval + delta);
        tvIntervalValue.setText(String.valueOf(config.interval));
        updatePreview();
    }

    private void toggleWeekday(String dayCode, TextView chip) {
        if (config.byDay == null) {
            config.byDay = new ArrayList<>();
        }
        if (config.byDay.contains(dayCode)) {
            config.byDay.remove(dayCode);
        } else {
            config.byDay.add(dayCode);
        }

        if (config.byDay.isEmpty()) {
            config.applyStartDefaults(startTimeMillis);
        }

        updateWeekdayChips();
        updatePreview();
    }

    private void updateWeekdayChips() {
        setDayChipState(chipMon, "MO");
        setDayChipState(chipTue, "TU");
        setDayChipState(chipWed, "WE");
        setDayChipState(chipThu, "TH");
        setDayChipState(chipFri, "FR");
        setDayChipState(chipSat, "SA");
        setDayChipState(chipSun, "SU");
    }

    private void setDayChipState(TextView chip, String code) {
        boolean selected = config.byDay != null && config.byDay.contains(code);
        chip.setBackgroundResource(selected ? R.drawable.bg_recurrence_day_selected
                : R.drawable.bg_recurrence_day_unselected);
        int color = selected ? android.R.color.white : R.color.slate_600;
        chip.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void showPositionPicker() {
        String[] labels = new String[]{"Thứ nhất", "Thứ hai", "Thứ ba", "Thứ tư", "Cuối cùng"};
        int[] values = new int[]{1, 2, 3, 4, -1};

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Vị trí")
                .setItems(labels, (dialog, which) -> {
                    config.setPos = values[which];
                    config.byMonthDay = null;
                    tvPositionValue.setText(labels[which]);
                    updateAdvancedExample();
                    updatePreview();
                })
                .show();
    }

    private void showDayPicker() {
        String[] labels = new String[]{"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        String[] codes = new String[]{"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Ngày")
                .setItems(labels, (dialog, which) -> {
                    if (config.byDay == null) {
                        config.byDay = new ArrayList<>();
                    }
                    config.byDay.clear();
                    config.byDay.add(codes[which]);
                    tvDayValue.setText(labels[which]);
                    updateAdvancedExample();
                    updatePreview();
                })
                .show();
    }

    private void updateAdvancedFields() {
        if (!RecurrenceUtils.FREQ_MONTHLY.equals(config.frequency)) {
            return;
        }

        if (config.setPos == null || config.setPos == 0) {
            config.setPos = 1;
        }
        if (config.byDay == null || config.byDay.isEmpty()) {
            config.applyStartDefaults(startTimeMillis);
        }

        if (config.byDay.size() > 1) {
            String first = config.byDay.get(0);
            config.byDay.clear();
            config.byDay.add(first);
        }

        tvPositionValue.setText(positionLabel(config.setPos));
        tvDayValue.setText(dayLabel(config.byDay.get(0)));
        updateAdvancedExample();
    }

    private void updateAdvancedExample() {
        if (!RecurrenceUtils.FREQ_MONTHLY.equals(config.frequency)) {
            return;
        }
        String example = "Mẫu: " + dayLabel(config.byDay.get(0)) + " " + positionSuffix(config.setPos)
                + " mỗi tháng";
        tvAdvancedExample.setText(example);
    }

    private String positionLabel(int setPos) {
        switch (setPos) {
            case 1:
                return "Thứ nhất";
            case 2:
                return "Thứ hai";
            case 3:
                return "Thứ ba";
            case 4:
                return "Thứ tư";
            case -1:
                return "Cuối cùng";
            default:
                return "Thứ nhất";
        }
    }

    private String positionSuffix(int setPos) {
        switch (setPos) {
            case 1:
                return "đầu tiên";
            case 2:
                return "thứ hai";
            case 3:
                return "thứ ba";
            case 4:
                return "thứ tư";
            case -1:
                return "cuối cùng";
            default:
                return "đầu tiên";
        }
    }

    private String dayLabel(String dayCode) {
        if (dayCode == null) {
            return "Thứ Hai";
        }
        switch (dayCode) {
            case "MO":
                return "Thứ Hai";
            case "TU":
                return "Thứ Ba";
            case "WE":
                return "Thứ Tư";
            case "TH":
                return "Thứ Năm";
            case "FR":
                return "Thứ Sáu";
            case "SA":
                return "Thứ Bảy";
            case "SU":
                return "Chủ Nhật";
            default:
                return "Thứ Hai";
        }
    }

    private void showExceptionPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 0, 0, 0);
            String stored = EXCEPTION_STORE.format(picked.getTime());
            if (!config.exceptions.contains(stored)) {
                config.exceptions.add(stored);
                updateExceptions();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateExceptions() {
        llExceptionsContainer.removeAllViews();
        if (config.exceptions == null || config.exceptions.isEmpty()) {
            return;
        }
        for (String exception : config.exceptions) {
            llExceptionsContainer.addView(buildExceptionBadge(exception));
        }
    }

    private View buildExceptionBadge(String storedDate) {
        LinearLayout badge = new LinearLayout(requireContext());
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setBackgroundResource(R.drawable.bg_recurrence_badge);
        badge.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(32));
        params.setMarginEnd(dpToPx(8));
        badge.setLayoutParams(params);

        TextView label = new TextView(requireContext());
        label.setText(formatDisplayDate(storedDate));
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_700));
        label.setTextSize(12);
        label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);

        ImageView remove = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        iconParams.setMarginStart(dpToPx(6));
        remove.setLayoutParams(iconParams);
        remove.setImageResource(R.drawable.ic_close);
        remove.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue_600));
        remove.setOnClickListener(v -> {
            config.exceptions.remove(storedDate);
            updateExceptions();
        });

        badge.addView(label);
        badge.addView(remove);
        return badge;
    }

    private String formatDisplayDate(String storedDate) {
        if (storedDate == null) {
            return "";
        }
        try {
            Date parsed = EXCEPTION_STORE.parse(storedDate);
            return DATE_DISPLAY.format(parsed);
        } catch (ParseException e) {
            return storedDate;
        }
    }

    private void updateEndUi() {
        if (config.endType == RecurrenceConfig.EndType.UNTIL) {
            rbOnDate.setChecked(true);
        } else if (config.endType == RecurrenceConfig.EndType.COUNT) {
            rbAfterCount.setChecked(true);
        } else {
            rbNever.setChecked(true);
        }

        layoutEndDateRow.setVisibility(config.endType == RecurrenceConfig.EndType.UNTIL ? View.VISIBLE : View.GONE);
        layoutEndCountRow.setVisibility(config.endType == RecurrenceConfig.EndType.COUNT ? View.VISIBLE : View.GONE);

        if (config.endType == RecurrenceConfig.EndType.UNTIL) {
            ensureUntilDate();
            tvEndDateValue.setText(DATE_DISPLAY.format(config.until));
        }
        if (config.endType == RecurrenceConfig.EndType.COUNT) {
            tvCountValue.setText(String.valueOf(Math.max(1, config.count)));
        }
    }

    private void ensureUntilDate() {
        if (config.until == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(startTimeMillis);
            calendar.add(Calendar.MONTH, 1);
            config.until = calendar.getTime();
        }
    }

    private void showEndDatePicker() {
        ensureUntilDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(config.until);
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 0, 0, 0);
            config.until = picked.getTime();
            tvEndDateValue.setText(DATE_DISPLAY.format(config.until));
            updatePreview();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateCount(int delta) {
        config.count = Math.max(1, config.count + delta);
        tvCountValue.setText(String.valueOf(config.count));
        updatePreview();
    }

    private void updatePreview() {
        tvPreview.setText(RecurrenceTextFormatter.formatSummary(config, startTimeMillis, true));
    }

    private void confirmAndClose() {
        config.enabled = true;
        config.applyStartDefaults(startTimeMillis);
        if (listener != null) {
            listener.onConfirmed(config);
        }
        dismiss();
    }

    private void setPillState(TextView pill, boolean selected) {
        pill.setBackgroundResource(selected ? R.drawable.bg_recurrence_pill_active
                : R.drawable.bg_recurrence_pill_inactive);
        int color = selected ? android.R.color.white : R.color.blue_700;
        pill.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
