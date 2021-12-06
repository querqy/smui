package controllers

object SignupForm {
  import play.api.data.Form
  import play.api.data.Forms._

  /**
    * A form processing DTO that maps to the form below.
    *
    * Using a class specifically for form binding reduces the chances
    * of a parameter tampering attack and makes code clearer.
    */
  case class UserData(name:String, email: String, password: String, password_confirmation: String)

  /**
    * The form definition for the "create a widget" form.
    * It specifies the form fields and their types,
    * as well as how to convert from a Data to form data and vice versa.
    */
  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "password"  -> nonEmptyText,
      "password_confirmation"  -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )
}

