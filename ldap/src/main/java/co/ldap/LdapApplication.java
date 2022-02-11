package co.ldap;


public class LdapApplication {

	public static void main(String[] args) {
		
		for(int i=0;i<1;i++){
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					
			
					String estado = Ldap.getInstance().getAuthentication("user", "pass").toString();
					System.out.println("Estado: " + estado);
				}
			});

			t.start();
		}	 
			

		}

	}

