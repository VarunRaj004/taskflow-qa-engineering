import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'

const STATUS_OPTIONS = ['TODO', 'IN_PROGRESS', 'DONE']
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH']

const emptyForm = { title: '', description: '', status: 'TODO', priority: 'MEDIUM' }

export default function TasksPage() {
  const [tasks, setTasks] = useState([])
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')

  function loadTasks() {
    api
      .listTasks({ search, status: statusFilter, priority: priorityFilter })
      .then((data) => {
        setTasks(data.tasks)
        setLoadError('')
      })
      .catch((err) => {
        setLoadError(err instanceof ApiError ? err.message : 'Failed to load tasks')
      })
  }

  useEffect(() => {
    loadTasks()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, statusFilter, priorityFilter])

  function openCreateForm() {
    setEditingId(null)
    setForm(emptyForm)
    setFormError('')
    setShowForm(true)
  }

  function openEditForm(task) {
    setEditingId(task.id)
    setForm({
      title: task.title,
      description: task.description || '',
      status: task.status,
      priority: task.priority,
    })
    setFormError('')
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditingId(null)
    setForm(emptyForm)
    setFormError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    try {
      if (editingId) {
        await api.updateTask(editingId, form)
      } else {
        await api.createTask(form)
      }
      closeForm()
      loadTasks()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to save task')
    }
  }

  async function handleDelete(task) {
    try {
      await api.deleteTask(task.id)
      loadTasks()
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : 'Failed to delete task')
    }
  }

  return (
    <div>
      <h1>Tasks</h1>

      <div className="filters">
        <div className="field">
          <label htmlFor="search">Search</label>
          <input
            id="search"
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All</option>
            {STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="priority-filter">Priority</label>
          <select
            id="priority-filter"
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
          >
            <option value="">All</option>
            {PRIORITY_OPTIONS.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </select>
        </div>
      </div>

      <button type="button" onClick={openCreateForm}>
        New Task
      </button>

      {loadError && <p role="alert" className="form-error">{loadError}</p>}

      {showForm && (
        <form onSubmit={handleSubmit} className="task-form">
          <div className="field">
            <label htmlFor="task-title">Title</label>
            <input
              id="task-title"
              type="text"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="task-description">Description</label>
            <textarea
              id="task-description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="task-status">Status</label>
            <select
              id="task-status"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value })}
            >
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="task-priority">Priority</label>
            <select
              id="task-priority"
              value={form.priority}
              onChange={(e) => setForm({ ...form, priority: e.target.value })}
            >
              {PRIORITY_OPTIONS.map((priority) => (
                <option key={priority} value={priority}>
                  {priority}
                </option>
              ))}
            </select>
          </div>
          {formError && <p role="alert" className="form-error">{formError}</p>}
          <button type="submit">Save Task</button>
          <button type="button" onClick={closeForm}>
            Cancel
          </button>
        </form>
      )}

      <ul className="task-list">
        {tasks.map((task) => (
          <li key={task.id} className="task-row">
            <div className="task-info">
              <strong>{task.title}</strong>
              {task.description && <p>{task.description}</p>}
              <span>Status: {task.status}</span>
              <span>Priority: {task.priority}</span>
            </div>
            <div className="task-actions">
              <button
                type="button"
                aria-label={`Edit task: ${task.title}`}
                onClick={() => openEditForm(task)}
              >
                Edit
              </button>
              <button
                type="button"
                aria-label={`Delete task: ${task.title}`}
                onClick={() => handleDelete(task)}
              >
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
