import Route from '@ember/routing/route';

export default class UsersRoute extends Route {
  async model() {
    try {
      const response = await fetch('http://localhost:8080/backend_war_exploded/UserServlet');
      // console.log(response.statusText,"Just for testing----------->")
      if (!response.ok) { // !200
        throw new Error(`Failed to fetch users: ${response.statusText}`);
      }
      const users = await response.json();
      // console.log(users ,"user");
      return users; 
    } catch (error) {
      console.error('Error fetching users:', error);
      return [];
    }
  }

  setupController(controller, model) {
    super.setupController(controller, model);
    controller.users = model;
  }
}
