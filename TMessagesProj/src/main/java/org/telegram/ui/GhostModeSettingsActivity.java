package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ListView.AdapterWithDiffUtils;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Objects;

public class GhostModeSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private final static int VIEW_TYPE_CHECK = 0;
    private final static int VIEW_TYPE_SHADOW = 1;

    private final static int hideOnlineStatusRow = 1;
    private final static int secretlyReadMessagesRow = 2;
    private final static int hideTypingStatusRow = 3;
    private final static int hideStoryViewsRow = 4;
    private final static int infoRow = 5;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.GhostMode));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutAnimation(null);
        listView.setAdapter(adapter = new ListAdapter());
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            if (item.id == hideOnlineStatusRow) {
                SharedConfig.toggleHideOnlineStatus();
                adapter.notifyDataSetChanged();
            } else if (item.id == secretlyReadMessagesRow) {
                SharedConfig.toggleSecretlyReadMessages();
                ((TextCheckCell) view).setChecked(SharedConfig.secretlyReadMessages);
            } else if (item.id == hideTypingStatusRow) {
                SharedConfig.toggleHideTypingStatus();
                ((TextCheckCell) view).setChecked(SharedConfig.hideTypingStatus);
            } else if (item.id == hideStoryViewsRow) {
                SharedConfig.toggleHideStoryViews();
                ((TextCheckCell) view).setChecked(SharedConfig.hideStoryViews);
            }
        });

        updateItems(false);

        return fragmentView;
    }

    private final ArrayList<ItemInner> oldItems = new ArrayList<>(), items = new ArrayList<>();

    private void updateItems(boolean animated) {
        oldItems.clear();
        oldItems.addAll(items);
        items.clear();

        items.add(new ItemInner(VIEW_TYPE_CHECK, hideOnlineStatusRow, LocaleController.getString(R.string.HideOnlineStatus)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, secretlyReadMessagesRow, LocaleController.getString(R.string.SecretlyReadMessages)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, hideTypingStatusRow, LocaleController.getString(R.string.HideTypingStatus)));
        items.add(new ItemInner(VIEW_TYPE_CHECK, hideStoryViewsRow, LocaleController.getString(R.string.HideStoryViews)));
        items.add(new ItemInner(VIEW_TYPE_SHADOW, infoRow, LocaleController.getString(R.string.GhostModeInfo)));

        if (adapter == null) {
            return;
        }

        if (animated) {
            adapter.setItems(oldItems, items);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private static class ItemInner extends AdapterWithDiffUtils.Item {
        public CharSequence text;
        public int id;
        public ItemInner(int viewType, int id, CharSequence text) {
            super(viewType, false);
            this.id = id;
            this.text = text;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemInner item = (ItemInner) o;
            return id == item.id && Objects.equals(text, item.text);
        }
    }

    private class ListAdapter extends AdapterWithDiffUtils {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(getContext());
            } else {
                view = new TextInfoPrivacyCell(getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            final boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == item.viewType;
            if (holder.getItemViewType() == VIEW_TYPE_SHADOW) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (TextUtils.isEmpty(item.text)) {
                    cell.setFixedSize(12);
                    cell.setText(null);
                } else {
                    cell.setFixedSize(0);
                    cell.setText(item.text);
                }
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                boolean checked;
                String about;
                if (item.id == hideOnlineStatusRow) {
                    checked = SharedConfig.hideOnlineStatus;
                    about = LocaleController.getString(R.string.HideOnlineStatusAbout);
                } else if (item.id == secretlyReadMessagesRow) {
                    checked = SharedConfig.secretlyReadMessages;
                    about = LocaleController.getString(R.string.SecretlyReadMessagesAbout);
                } else if (item.id == hideTypingStatusRow) {
                    checked = SharedConfig.hideTypingStatus;
                    about = LocaleController.getString(R.string.HideTypingStatusAbout);
                } else if (item.id == hideStoryViewsRow) {
                    checked = SharedConfig.hideStoryViews;
                    about = LocaleController.getString(R.string.HideStoryViewsAbout);
                } else {
                    return;
                }
                cell.setTextAndValueAndCheck(item.text.toString(), about, checked, true, divider);
                cell.setEnabled(!(item.id == secretlyReadMessagesRow && SharedConfig.hideOnlineStatus));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() != VIEW_TYPE_SHADOW;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return 0;
            }
            return items.get(position).viewType;
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        listView.setPadding(0, 0, 0, bottom);
        listView.setClipToPadding(false);
    }
}
