package com.fastaccess.ui.modules.repos.code.commit;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import com.fastaccess.R;
import com.fastaccess.data.dao.BranchesModel;
import com.fastaccess.helper.BundleConstant;
import com.fastaccess.helper.Bundler;
import com.fastaccess.helper.InputHelper;
import com.fastaccess.provider.rest.loadmore.OnLoadMore;
import com.fastaccess.ui.adapter.CommitsAdapter;
import com.fastaccess.ui.base.BaseFragment;
import com.fastaccess.ui.modules.repos.RepoPagerMvp;
import com.fastaccess.ui.widgets.StateLayout;
import com.fastaccess.ui.widgets.recyclerview.DynamicRecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.OnItemSelected;

/**
 * Created by Kosh on 03 Dec 2016, 3:56 PM
 */

public class RepoCommitsView extends BaseFragment<RepoCommitsMvp.View, RepoCommitsPresenter> implements RepoCommitsMvp.View {
    @BindView(R.id.recycler) DynamicRecyclerView recycler;
    @BindView(R.id.refresh) SwipeRefreshLayout refresh;
    @BindView(R.id.stateLayout) StateLayout stateLayout;
    @BindView(R.id.branches) AppCompatSpinner branches;
    @BindView(R.id.branchesProgress) ProgressBar branchesProgress;
    private OnLoadMore onLoadMore;
    private CommitsAdapter adapter;
    private RepoPagerMvp.View repoCallback;
    private RepoPagerMvp.TabsBadgeListener tabsBadgeListener;

    public static RepoCommitsView newInstance(@NonNull String repoId, @NonNull String login, @NonNull String branch) {
        RepoCommitsView view = new RepoCommitsView();
        view.setArguments(Bundler.start()
                .put(BundleConstant.ID, repoId)
                .put(BundleConstant.EXTRA, login)
                .put(BundleConstant.EXTRA_TWO, branch)
                .end());
        return view;
    }

    @OnItemSelected(R.id.branches) void onBranchSelected(int position) {
        if (repoCallback.hasUserInteractedWithView()) {
            String ref = ((BranchesModel) branches.getItemAtPosition(position)).getName();
            getPresenter().onBranchChanged(ref);
        }
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RepoPagerMvp.View) {
            repoCallback = (RepoPagerMvp.View) context;
        } else if (getParentFragment() instanceof RepoPagerMvp.View) {
            repoCallback = (RepoPagerMvp.View) getParentFragment();
        }
        if (context instanceof RepoPagerMvp.TabsBadgeListener) {
            tabsBadgeListener = (RepoPagerMvp.TabsBadgeListener) context;
        } else if (getParentFragment() instanceof RepoPagerMvp.TabsBadgeListener) {
            tabsBadgeListener = (RepoPagerMvp.TabsBadgeListener) getParentFragment();
        }
    }

    @Override public void onDetach() {
        repoCallback = null;
        super.onDetach();
    }

    @Override public void onNotifyAdapter() {

        hideProgress();
        adapter.notifyDataSetChanged();
    }

    @Override protected int fragmentLayout() {
        return R.layout.commit_with_branch_layout;
    }

    @Override protected void onFragmentCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() == null) {
            throw new NullPointerException("Bundle is null, therefore, issues can't be proceeded.");
        }
        stateLayout.setEmptyText(R.string.no_commits);
        stateLayout.setOnReloadListener(this);
        refresh.setOnRefreshListener(this);
        recycler.setEmptyView(stateLayout, refresh);
        adapter = new CommitsAdapter(getPresenter().getCommits());
        adapter.setListener(getPresenter());
        getLoadMore().setCurrent_page(getPresenter().getCurrentPage(), getPresenter().getPreviousTotal());
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(getLoadMore());
        if (savedInstanceState == null) {
            getPresenter().onFragmentCreated(getArguments());
        } else if (getPresenter().getCommits().isEmpty() && !getPresenter().isApiCalled()) {
            onRefresh();
        }
        setBranchesData(getPresenter().getBranches(), false);
    }

    @NonNull @Override public RepoCommitsPresenter providePresenter() {
        return new RepoCommitsPresenter();
    }

    @Override public void showProgress(@StringRes int resId) {

        stateLayout.showProgress();
    }

    @Override public void hideProgress() {
        refresh.setRefreshing(false);
        stateLayout.hideProgress();
    }

    @Override public void showErrorMessage(@NonNull String message) {
        showReload();
        super.showErrorMessage(message);
    }

    @Override public void showMessage(int titleRes, int msgRes) {
        showReload();
        super.showMessage(titleRes, msgRes);
    }

    @SuppressWarnings("unchecked") @NonNull @Override public OnLoadMore getLoadMore() {
        if (onLoadMore == null) {
            onLoadMore = new OnLoadMore(getPresenter());
        }
        return onLoadMore;
    }

    @Override public void setBranchesData(@NonNull List<BranchesModel> branchesData, boolean firstTime) {
        branchesProgress.setVisibility(View.GONE);
        ArrayAdapter<BranchesModel> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, branchesData);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        branches.setAdapter(adapter);
        if (firstTime) {
            if (!InputHelper.isEmpty(getPresenter().getDefaultBranch())) {
                int index = -1;
                for (int i = 0; i < branchesData.size(); i++) {
                    if (branchesData.get(i).getName().equals(getPresenter().getDefaultBranch())) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    branches.setSelection(index, true);
                }
            }
        }
    }

    @Override public void showBranchesProgress() {
        branchesProgress.setVisibility(View.VISIBLE);
    }

    @Override public void hideBranchesProgress() {
        branchesProgress.setVisibility(View.GONE);
    }

    @Override public void onShowCommitCount(long sum) {
        if (tabsBadgeListener != null) {
            tabsBadgeListener.onSetBadge(2, (int) sum);
        }
    }

    @Override public void onRefresh() {
        getPresenter().onCallApi(1, null);
    }

    @Override public void onClick(View view) {
        onRefresh();
    }

    private void showReload() {
        hideBranchesProgress();
        hideProgress();
        stateLayout.showReload(adapter.getItemCount());
    }
}
