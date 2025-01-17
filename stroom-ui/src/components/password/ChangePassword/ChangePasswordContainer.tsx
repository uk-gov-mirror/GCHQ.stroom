/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// TODO : @66 FIX THIS
import * as queryString from "query-string";
import * as React from "react";
import { useEffect, useState } from "react";
import useRouter from "lib/useRouter";
import ChangePasswordForm from "./ChangePasswordForm";
import usePassword from "./useChangePassword";

const ChangePasswordContainer = () => {
  const {
    changePassword,
    showChangeConfirmation,
    isSubmitting,
  } = usePassword();
  const { router } = useRouter();
  const [redirectUri, setRedirectUri] = useState("");
  const [email, setEmail] = useState("");
  // const { apiUrl } = useUrlFactory();
  // const resource = apiUrl("/Oldauthentication/v1");

  useEffect(() => {
    if (!!router.location) {
      const query = queryString.parse(router.location.search);

      const redirectUri: string = query.redirect_uri as string;
      if (!!redirectUri) {
        const decodedRedirectUri: string = decodeURIComponent(redirectUri);
        setRedirectUri(decodedRedirectUri);
      }

      if (email) {
        setEmail(email);
      } else {
        console.error(
          "Unable to display the change password page because we could not get the user's email address from either the query string or a cookie!",
        );
      }
    }

    // Try and get the user's email from the query string, and fall back on a cookie.
  }, [router.location, setRedirectUri, email, setEmail]);

  // const handleValidate = (
  //   oldPassword: string,
  //   newPassword: string,
  //   verifyPassword: string,
  //   email: string,
  // ) => {
  //   return validateAsync(
  //     email,
  //     newPassword,
  //     verifyPassword,
  //     resource,
  //     oldPassword,
  //   );
  // };

  return (
    <ChangePasswordForm
      isSubmitting={isSubmitting}
      onSubmit={changePassword}
      redirectUri={redirectUri}
      email={email}
      showChangeConfirmation={showChangeConfirmation}
      // onValidate={handleValidate}
    />
  );
};

export default ChangePasswordContainer;
