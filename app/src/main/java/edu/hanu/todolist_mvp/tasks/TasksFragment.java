package edu.hanu.todolist_mvp.tasks;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import edu.hanu.todolist_mvp.R;
import edu.hanu.todolist_mvp.addedittask.AddEditTaskActivity;
import edu.hanu.todolist_mvp.data.Task;
import edu.hanu.todolist_mvp.taskdetail.TaskDetailActivity;

public class TasksFragment extends Fragment implements TasksContract.View {

    private TasksContract.Presenter mPresenter;

    private TasksAdapter mListAdapter;

    private View mNoTasksView;

    private ImageView mNoTaskIcon;

    private TextView mNoTaskMainView;

    private TextView mNoTaskAddView;

    private LinearLayout mTasksView;

    private TextView mFilteringLabelView;

    public TasksFragment() {
        // Requires empty public constructor
    }

    public static TasksFragment newInstance() {
        return new TasksFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListAdapter = new TasksAdapter(new ArrayList<Task>(0), mItemListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.start();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setPresenter(@NonNull TasksContract.Presenter presenter) {
        this.mPresenter = Preconditions.checkNotNull(presenter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPresenter.result(requestCode, resultCode);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tasks_frag, container, false);

        // Set up tasks view
        ListView listView = (ListView) root.findViewById(R.id.tasks_list);
        listView.setAdapter(mListAdapter);
        mFilteringLabelView = (TextView) root.findViewById(R.id.filteringLabel);
        mTasksView = (LinearLayout) root.findViewById(R.id.tasksLL);

        // Set up  no tasks view
        mNoTasksView = root.findViewById(R.id.noTasks);
        mNoTaskIcon = (ImageView) root.findViewById(R.id.noTasksIcon);
        mNoTaskMainView = (TextView) root.findViewById(R.id.noTasksMain);
        mNoTaskAddView = (TextView) root.findViewById(R.id.noTasksAdd);
        mNoTaskAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTask();
            }
        });

        // Set up floating action button
        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_add_task);

        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.addNewTask();
            }
        });

        // Set up progress indicator
        final ScrollChildSwipeRefreshLayout swipeRefreshLayout =
                (ScrollChildSwipeRefreshLayout) root.findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.header),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
        );
        // Set the scrolling view in the custom SwipeRefreshLayout.
        swipeRefreshLayout.setScrollUpChild(listView);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadTasks(false);
            }
        });

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                mPresenter.clearCompletedTasks();
                break;
            case R.id.menu_filter:
                showFilteringPopUpMenu();
                break;
            case R.id.menu_refresh:
                mPresenter.loadTasks(true);
                break;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tasks_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void showFilteringPopUpMenu() {
        PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter));
        popup.getMenuInflater().inflate(R.menu.filter_tasks, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.active:
                        mPresenter.setFiltering(TasksFilterType.ACTIVE_TASKS);
                        break;
                    case R.id.completed:
                        mPresenter.setFiltering(TasksFilterType.COMPLETED_TASKS);
                        break;
                    default:
                        mPresenter.setFiltering(TasksFilterType.ALL_TASKS);
                        break;
                }
                mPresenter.loadTasks(false);
                return true;
            }
        });

        popup.show();
    }

    /**
     * Listener for clicks on tasks in the ListView.
     */
    TaskItemListener mItemListener = new TaskItemListener() {
        @Override
        public void onTaskClick(Task clickedTask) {
            mPresenter.openTaskDetails(clickedTask);
        }

        @Override
        public void onCompleteTaskClick(Task completedTask) {
            mPresenter.completeTask(completedTask);
        }

        @Override
        public void onActivateTaskClick(Task activatedTask) {
            mPresenter.activateTask(activatedTask);
        }
    };

    @Override
    public void setLoadingIndicator(final boolean active) {

        if (getView() == null) {
            return;
        }
        final ScrollChildSwipeRefreshLayout srl =
                (ScrollChildSwipeRefreshLayout) getView().findViewById(R.id.refresh_layout);

        // Make sure setRefreshing() is called after the layout is done with everything else.
        srl.post(new Runnable() {
            @Override
            public void run() {
                srl.setRefreshing(active);
            }
        });
    }

    @Override
    public void showTasks(List<Task> tasks) {
        mListAdapter.replaceData(tasks);

        mTasksView.setVisibility(View.VISIBLE);
        mNoTasksView.setVisibility(View.GONE);
    }

    @Override
    public void showNoActiveTasks() {
        showNoTasksViews(
                getResources().getString(R.string.no_tasks_active),
                R.drawable.ic_check_circle_24dp,
                false
        );
    }

    @Override
    public void showNoTasks() {
        showNoTasksViews(
                getResources().getString(R.string.no_tasks_all),
                R.drawable.ic_assignment_turned_in_24dp,
                false
        );
    }

    @Override
    public void showNoCompletedTasks() {
        showNoTasksViews(
                getResources().getString(R.string.no_tasks_completed),
                R.drawable.ic_verified_user_24dp,
                false
        );
    }

    @Override
    public void showSuccessfullySavedMessage() {
        showMessage(getString(R.string.successfully_saved_task_message));
    }

    private void showNoTasksViews(String mainText, int iconRes, boolean showAddView) {
        mTasksView.setVisibility(View.GONE);
        mNoTasksView.setVisibility(View.VISIBLE);

        mNoTaskMainView.setText(mainText);
        mNoTaskIcon.setImageDrawable(getResources().getDrawable(iconRes));
        mNoTaskAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showActiveFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_active));
    }

    @Override
    public void showCompletedFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_completed));
    }

    @Override
    public void showAllFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_all));
    }

    @Override
    public void showAddTask() {
        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
        startActivityForResult(intent, AddEditTaskActivity.REQUEST_ADD_TASK);
    }

    @Override
    public void showTaskDetailsUi(String taskId) {
        // in it's own Activity, since it makes more sense that way and it gives us the flexibility
        // to show some Intent stubbing.
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
        startActivity(intent);
    }

    @Override
    public void showTaskMarkedComplete() {
        showMessage(getString(R.string.task_marked_complete));
    }

    @Override
    public void showTaskMarkedActive() {
        showMessage(getString(R.string.task_marked_active));
    }

    @Override
    public void showCompletedTasksCleared() {
        showMessage(getString(R.string.completed_tasks_cleared));
    }

    @Override
    public void showLoadingTasksError() {
        showMessage(getString(R.string.loading_tasks_error));
    }

    private void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    private static class TasksAdapter extends BaseAdapter {

        private List<Task> mTasks;
        private TaskItemListener mItemListener;

        public TasksAdapter(List<Task> tasks, TaskItemListener itemListener) {
            setList(tasks);
            mItemListener = itemListener;
        }

        public void replaceData(List<Task> tasks) {
            setList(tasks);
            notifyDataSetChanged();
        }

        @SuppressLint("RestrictedApi")
        private void setList(List<Task> tasks) {
            mTasks = Preconditions.checkNotNull(tasks);
        }

        @Override
        public int getCount() {
            return mTasks.size();
        }

        @Override
        public Task getItem(int i) {
            return mTasks.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View rowView = view;
            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                rowView = inflater.inflate(R.layout.task_item, viewGroup, false);
            }

            final Task task = getItem(i);

            TextView titleTV = (TextView) rowView.findViewById(R.id.title);
            titleTV.setText(task.getTitleForList());

            CheckBox completeCB = (CheckBox) rowView.findViewById(R.id.complete);

            // Active/completed task UI
            completeCB.setChecked(task.isCompleted());
            if (task.isCompleted()) {
                rowView.setBackgroundDrawable(viewGroup.getContext()
                        .getResources().getDrawable(R.drawable.list_completed_touch_feedback));
            } else {
                rowView.setBackgroundDrawable(viewGroup.getContext()
                        .getResources().getDrawable(R.drawable.touch_feedback));
            }

            completeCB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!task.isCompleted()) {
                        mItemListener.onCompleteTaskClick(task);
                    } else {
                        mItemListener.onActivateTaskClick(task);
                    }
                }
            });

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemListener.onTaskClick(task);
                }
            });

            return rowView;
        }
    }

    public interface TaskItemListener {

        void onTaskClick(Task clickedTask);

        void onCompleteTaskClick(Task completedTask);

        void onActivateTaskClick(Task activatedTask);
    }

//    private TasksContract.Presenter presenter;
//    private TasksAdapter tasksAdapter;
//    private View noTasksView;
//    private ImageView noTaskIcon;
//    private TextView noTaskMainView, filteringLabelView;
//    private LinearLayout tasksView;
//    private RecyclerView recyclerView;



//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        presenter.start();
//    }
//
//    @SuppressLint("RestrictedApi")
//    @Override
//    public void setPresenter(TasksContract.Presenter presenter) {
//        this.presenter = checkNotNull(presenter);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        presenter.result(requestCode, resultCode);
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View root = inflater.inflate(R.layout.tasks_frag, container, false);
//
//        // Set up tasks view
//        recyclerView = root.findViewById(R.id.tasks_list);
//        tasksAdapter = new TasksAdapter(new ArrayList<>(0), itemListener);
//        LinearLayoutManager manager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
//        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
//        recyclerView.addItemDecoration(itemDecoration);
//        recyclerView.setLayoutManager(manager);
//        recyclerView.setAdapter(tasksAdapter);
//        filteringLabelView = root.findViewById(R.id.filteringLabel);
//        tasksView = root.findViewById(R.id.tasksLL);
//
//        //Set up no tasks view
//        noTasksView = root.findViewById(R.id.noTasks);
//        noTaskIcon = root.findViewById(R.id.noTasksIcon);
//        noTaskMainView = root.findViewById(R.id.noTasksMain);
//
//        // Set up floating action button
//        FloatingActionButton fab = getActivity().findViewById(R.id.fab_add_task);
//        fab.setImageResource(R.drawable.ic_add);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                presenter.addNewTask();
//            }
//        });
//
//        final ScrollChildSwipeRefreshLayout swipeRefreshLayout =
//                root.findViewById(R.id.refresh_layout);
//        swipeRefreshLayout.setColorSchemeColors(
//                ContextCompat.getColor(getActivity(), R.color.header),
//                ContextCompat.getColor(getActivity(), R.color.colorAccent),
//                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
//        );
//
//        // Set the scrolling view in the custom SwipeRefreshLayout
//        swipeRefreshLayout.setScrollUpChild(recyclerView);
//
//        setHasOptionsMenu(true);
//
//        return root;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_clear:
//                presenter.clearCompletedTasks();
//                break;
//            case R.id.menu_filter:
//                showFilteringPopUpMenu();
//                break;
//            case R.id.menu_refresh:
//                presenter.loadTasks(true);
//                break;
//        }
//        return true;
//    }
//
//    @Override
//    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.tasks_fragment_menu, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }
//
//    TaskItemListener itemListener = new TaskItemListener() {
//
//        @Override
//        public void onTaskClick(Task clickedTask) {
//            presenter.openTaskDetails(clickedTask);
//        }
//
//        @Override
//        public void onCompleteTaskClick(Task completedTask) {
//            presenter.completeTask(completedTask);
//        }
//
//        @Override
//        public void onActivateTaskClick(Task activatedTask) {
//            presenter.activateTask(activatedTask);
//        }
//    };
//
//    @Override
//    public void setLoadingIndicator(boolean active) {
//        if (getView() == null) {
//            return;
//        }
//        final SwipeRefreshLayout srl = getView().findViewById(R.id.refresh_layout);
//        srl.post(new Runnable() {
//            @Override
//            public void run() {
//                srl.setRefreshing(active);
//            }
//        });
//    }
//
//    @Override
//    public void showTasks(List<Task> tasks) {
//        tasksAdapter.setData(tasks);
//
//
//        tasksView.setVisibility(View.VISIBLE);
//        noTasksView.setVisibility(View.GONE);
//    }
//
//    @Override
//    public void showAddTask() {
//        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
//        startActivityForResult(intent, AddEditTaskActivity.REQUEST_ADD_TASK);
//    }
//
//    @Override
//    public void showTaskDetailsUi(String taskId) {
//        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
//        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
//        startActivity(intent);
//    }
//
//    @Override
//    public void showTaskMarkedComplete() {
//        showMessage(getString(R.string.task_marked_complete));
//    }
//
//    @Override
//    public void showTaskMarkedActive() {
//        showMessage(getString(R.string.task_marked_active));
//    }
//
//    @Override
//    public void showCompletedTasksCleared() {
//        showMessage(getString(R.string.completed_tasks_cleared));
//    }
//
//    @Override
//    public void showLoadingTasksError() {
//        showMessage(getString(R.string.loading_tasks_error));
//    }
//
//    @Override
//    public void showNoTasks() {
//        showNoTasksView(
//                getResources().getString(R.string.no_tasks_all),
//                R.drawable.ic_assignment_turned_in_24dp,
//                false
//        );
//    }
//
//    @Override
//    public void showActiveFilterLabel() {
//        filteringLabelView.setText(getResources().getString(R.string.label_active));
//    }
//
//    @Override
//    public void showCompletedFilterLabel() {
//        filteringLabelView.setText(getResources().getString(R.string.label_completed));
//    }
//
//    @Override
//    public void showAllFilterLabel() {
//        filteringLabelView.setText(getResources().getString(R.string.label_all));
//    }
//
//    @Override
//    public void showNoActiveTasks() {
//        showNoTasksView(
//                getResources().getString(R.string.no_tasks_active),
//                R.drawable.ic_check_circle_24dp,
//                false
//        );
//    }
//
//    @Override
//    public void showNoCompletedTasks() {
//        showNoTasksView(
//                getResources().getString(R.string.no_tasks_completed),
//                R.drawable.ic_verified_user_24dp,
//                false
//        );
//    }
//
//    @Override
//    public void showSuccessfullySavedMessage() {
//        showMessage(getString(R.string.successfully_saved_task_message));
//    }
//
//    @Override
//    public boolean isActive() {
//        return isAdded();
//    }
//
//    @Override
//    public void showFilteringPopUpMenu() {
//        PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter));
//        popup.getMenuInflater().inflate(R.menu.filter_tasks, popup.getMenu());
//
//        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//                switch (menuItem.getItemId()) {
//                    case R.id.active:
//                        presenter.setFiltering(TasksFilterType.ACTIVE_TASKS);
//                        break;
//                    case R.id.completed:
//                        presenter.setFiltering(TasksFilterType.COMPLETED_TASKS);
//                        break;
//                    default:
//                        presenter.setFiltering(TasksFilterType.ALL_TASKS);
//                        break;
//                }
//                presenter.loadTasks(false);
//                return true;
//            }
//        });
//        popup.show();
//
//    }
//
//    private void showNoTasksView(String mainText, int iconRes, boolean showAddView ) {
//        tasksView.setVisibility(View.GONE);
//        noTasksView.setVisibility(View.VISIBLE);
//
//        noTaskMainView.setText(mainText);
//        noTaskIcon.setImageDrawable(getResources().getDrawable(iconRes));
//    }
//
//    private void showMessage(String message) {
//        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
//    }
//
//
//    public interface TaskItemListener {
//
//        void onTaskClick(Task clickedTask);
//
//        void onCompleteTaskClick(Task completedTask);
//
//        void onActivateTaskClick(Task activatedTask);
//    }
}