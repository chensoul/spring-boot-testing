package wf.garnier.springboottesting.todos.simple;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TodoService {

	private final TodoRepository todoRepository;

	public TodoService(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	public List<TodoItem> getTodos() {
		return todoRepository.findAll();
	}

	@Transactional
	public void delete(long id) {
		todoRepository.deleteById(id);
	}

	public TodoItem addTodo(String text, String description) {
		return todoRepository.save(new TodoItem(text, description));
	}

	public List<TodoItem> searchByKeyword(String keyword) {
		return todoRepository.searchByKeyword(keyword);
	}

}
