# SMUI FRONTEND

It's recommended to install the [Angular Client Tool](https://angular.io/cli) and [YARN](https://yarnpkg.com/getting-started/install) package manager.

## Starting the frontend
If you run `sbt run` in the root folder it will automatically start the Play-backend and Angular-frontend (both with hot-reloading).
If you for some reason need to start the frontend independently, install all dependencies via your package manager (e.g. `yarn install`) and run: `ng start`.

## Updating the dependencies
Run `ng update` to update the Angular application and its dependencies. (e.g. `ng update @angular/cli @angular/core`)
To update packages to their latest version use your package manager (e.g. `yarn upgrade`)

## Fix linting errors
This application is using [ESLint](https://eslint.org/). Use `yarn lint` to show current linting errors. 
If you automatically fix minor linting errors you can use: `yarn lint --fix`

## Testing
Run `yarn test` to execute the unit tests using [Karma](https://karma-runner.github.io) and headless Chrome.
Run `yarn coverage` to check the code test coverage.
Run `yarn e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Build
Run `yarn build:dev` or `yarn build:prod` to trigger the development or production build of the frontend.
The build artifacts will be stored in the `dist/` directory.
