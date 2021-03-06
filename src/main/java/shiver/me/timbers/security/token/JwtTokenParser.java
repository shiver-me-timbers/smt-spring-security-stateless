/*
 * Copyright 2015 Karl Bennett
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

package shiver.me.timbers.security.token;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static io.jsonwebtoken.SignatureAlgorithm.HS512;
import static java.lang.String.format;

/**
 * This is a very basic JWT token implementation, it doesn't even set an expiration date for the token so it will be
 * valid for ever.
 *
 * @author Karl Bennett
 */
public class JwtTokenParser<T> implements TokenParser<T> {

    private static final String ENTITY = "entity";

    private final Class<T> type;
    private final String secret;
    private final JwtBuilder jwtBuilder;
    private final JwtParser jwtParser;
    private final MessagePack messagePack;
    private final Base64 base64;
    private final Dates dates;
    private Long duration;
    private TimeUnit unit;

    public JwtTokenParser(Class<T> type, String secret) {
        this(
            type,
            secret,
            Jwts.builder(),
            Jwts.parser(),
            new MessagePack(),
            new DataConverterBase64(),
            new SystemDates()
        );
    }

    public JwtTokenParser(
        Class<T> type,
        String secret,
        JwtBuilder jwtBuilder,
        JwtParser jwtParser,
        MessagePack messagePack,
        Base64 base64,
        Dates dates
    ) {
        this.type = type;
        this.secret = secret;
        this.jwtBuilder = jwtBuilder;
        this.jwtParser = jwtParser;
        this.messagePack = messagePack;
        this.base64 = base64;
        this.dates = dates;
    }

    @Override
    public String create(T entity) {
        try {
            final JwtBuilder builder = configure(this.jwtBuilder)
                .claim(ENTITY, base64.encode(messagePack.write(entity)))
                .signWith(HS512, secret);

            if (duration != null) {
                builder.setExpiration(dates.nowPlus(duration, unit));
            }
            return builder
                .compact();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Override this method to add extra configuration to the {@link JwtBuilder}.
     */
    protected JwtBuilder configure(JwtBuilder jwtBuilder) {
        return jwtBuilder;
    }

    @Override
    public T parse(String token) {
        try {
            return messagePack.read(
                base64.decode(jwtParser.setSigningKey(secret).parseClaimsJws(token).getBody().get(ENTITY).toString()),
                type
            );
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (ExpiredJwtException e) {
            return null;
        }
    }

    public JwtTokenParser<T> willExpireAfter(Long duration, TimeUnit unit) {
        if (duration == null || unit == null) {
            throw new IllegalArgumentException(
                format("The token expiration must have both its duration (%s) and unit (%s) set.", duration, unit)
            );
        }
        this.duration = duration;
        this.unit = unit;
        return this;
    }
}
