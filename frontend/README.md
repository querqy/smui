# SMUI FRONTEND

## Starting the frontend
If you run `sbt run` in the root folder it will automatically start the Play-backend and Angular-frontend (both with hot-reloading).
If you for some reason need to start the frontend independently use `ǹg start`

## Updating the dependencies
Run `ng update` to update the Angular application and its dependencies.

## Fix linting errors
This application is using [ESLint](https://eslint.org/). Use `ng lint` to show current linting errors. 
If you automatically fix minor linting errors you can use: `ng lint --fix`

## Testing
Run `ng test` to execute the unit tests using [Karma](https://karma-runner.github.io).
Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Code scaffolding
Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.
